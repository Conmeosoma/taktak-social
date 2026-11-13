package com.taktak.server.ai;
import com.fasterxml.jackson.databind.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
@Component
public class GeminiProvider implements AiProvider {
    static final String SYSTEM="Bạn là Taktak AI, một trợ lý AI thân thiện, rõ ràng và hữu ích được tích hợp trong ứng dụng Taktak. Hãy ưu tiên trả lời bằng ngôn ngữ của người dùng, mặc định tiếng Việt. Không tự nhận có quyền truy cập ảnh, tin nhắn, danh bạ, hồ sơ hoặc dữ liệu riêng tư của họ khi những dữ liệu đó chưa được cung cấp hợp lệ. Không bịa thông tin về các chức năng của Taktak. Khi không chắc chắn, hãy nói rõ giới hạn.";
    private final AiSettings settings; private final GeminiKeyManager keys;private final ObjectMapper json;private final MeterRegistry metrics;
    private final HttpClient http;
    @org.springframework.beans.factory.annotation.Autowired
    public GeminiProvider(AiSettings settings,GeminiKeyManager keys,ObjectMapper json,MeterRegistry metrics){this(settings,keys,json,metrics,settings.enabled?HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build():null);}
    GeminiProvider(AiSettings settings,GeminiKeyManager keys,ObjectMapper json,MeterRegistry metrics,HttpClient http){this.settings=settings;this.keys=keys;this.json=json;this.metrics=metrics;this.http=http;}
    public String availability(){
        if(!settings.enabled)return "DISABLED";
        try{if(!settings.temperature.isBlank()){double t=Double.parseDouble(settings.temperature);if(!Double.isFinite(t)||t<0||t>2)return "CONFIGURATION_ERROR";}}
        catch(NumberFormatException e){return "CONFIGURATION_ERROR";}
        return !settings.model.matches("[a-zA-Z0-9.-]{1,80}")||!keys.configured()?"CONFIGURATION_ERROR":keys.available()?"CONFIGURED":"TEMPORARILY_UNAVAILABLE";
    }
    public Answer generate(List<Turn> context) {
        if(!availability().equals("CONFIGURED"))throw new AiFailure("AI_"+availability(),HttpStatus.SERVICE_UNAVAILABLE);
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(settings.timeout);
        for(int attempt=0;attempt<=settings.retries;attempt++) {
            GeminiKeyManager.Key key=keys.select(); long start=System.nanoTime();
            try {
                var contents=context.stream().map(t->Map.of("role",t.role().equals("assistant")?"model":"user","parts",List.of(Map.of("text",t.text())))).toList();
                var generation=new HashMap<String,Object>();generation.put("maxOutputTokens",settings.outputTokens);
                if(!settings.temperature.isBlank())generation.put("temperature",Double.parseDouble(settings.temperature));
                String body=json.writeValueAsString(Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",SYSTEM))),
                        "contents",contents,"generationConfig",generation));
                long remaining=deadline-System.nanoTime();if(remaining<=0)throw new HttpTimeoutException("AI deadline");
                var req=HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"+settings.model+":generateContent"))
                        .timeout(Duration.ofNanos(remaining)).header("x-goog-api-key",key.secret).header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                var response=http.send(req,info->new LimitedBody());
                String text=new String(response.body(),java.nio.charset.StandardCharsets.UTF_8);
                if(response.statusCode()!=200){
                    long retry=retryAfter(response.headers().firstValue("Retry-After").orElse(""));
                    var error=GeminiErrorClassifier.classify(response.statusCode(),text,retry);
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("AI upstream rejected request: status={} category={}",response.statusCode(),error.category());
                    keys.fail(key,error.category(),error.cooldown(),error.group(),error.invalid());
                    metrics.counter("taktak.ai.errors","category",error.category()).increment();
                    if(error.invalid() && attempt<settings.retries)continue;
                    if(error.retry() && attempt<settings.retries){pause(attempt,Math.max(retry,1),deadline);continue;}
                    throw new AiFailure(response.statusCode()==404?"AI_CONFIGURATION_ERROR":response.statusCode()==429?"AI_QUOTA_EXHAUSTED":response.statusCode()==400?"AI_INVALID_REQUEST":"AI_TEMPORARILY_UNAVAILABLE",
                            response.statusCode()==400?HttpStatus.BAD_REQUEST:response.statusCode()==429?HttpStatus.TOO_MANY_REQUESTS:HttpStatus.SERVICE_UNAVAILABLE,error.cooldown());
                }
                var node=json.readTree(text); var candidate=node.path("candidates").path(0);
                if(!Set.of("STOP","MAX_TOKENS").contains(candidate.path("finishReason").asText()))throw new AiFailure("AI_CONTENT_UNAVAILABLE",HttpStatus.UNPROCESSABLE_ENTITY);
                StringBuilder answer=new StringBuilder();
                for(var part:candidate.path("content").path("parts"))if(!part.path("thought").asBoolean(false))answer.append(part.path("text").asText(""));
                if(answer.isEmpty()||answer.length()>16000)throw new AiFailure("AI_CONTENT_UNAVAILABLE",HttpStatus.UNPROCESSABLE_ENTITY);
                int input=Math.max(0,node.path("usageMetadata").path("promptTokenCount").asInt());
                int output=Math.max(0,node.path("usageMetadata").path("candidatesTokenCount").asInt());
                keys.success(key);metrics.counter("taktak.ai.success").increment();metrics.counter("taktak.ai.tokens","kind","input").increment(input);
                metrics.counter("taktak.ai.tokens","kind","output").increment(output);
                return new Answer(answer.toString(),input,output);
            } catch(InterruptedException e){Thread.currentThread().interrupt();throw new AiFailure("AI_TEMPORARILY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE);}
            catch(IOException e){
                // Transport outcome may be ambiguous: do not automatically replay a generation.
                keys.fail(key,"TRANSPORT",5,false,false);metrics.counter("taktak.ai.errors","category","TRANSPORT").increment();
                throw new AiFailure("AI_TEMPORARILY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,5);
            } finally {metrics.timer("taktak.ai.latency").record(System.nanoTime()-start,TimeUnit.NANOSECONDS);}
        }
        throw new AiFailure("AI_TEMPORARILY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE);
    }
    static long retryAfter(String value){try{return Math.min(86400,Math.max(0,Long.parseLong(value)));}catch(Exception ignored){try{return Math.min(86400,Math.max(0,Duration.between(Instant.now(),ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds()));}catch(Exception e){return 0;}}}
    private void pause(int attempt,long minimum,long deadline)throws InterruptedException {
        long millis=Math.max(minimum*1000,1000L*(1L<<attempt)+ThreadLocalRandom.current().nextLong(250));
        if(System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis)>=deadline)throw new AiFailure("AI_TEMPORARILY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,minimum);
        Thread.sleep(millis);
    }
    static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        final CompletableFuture<byte[]> body=new CompletableFuture<>(); final ByteArrayOutputStream bytes=new ByteArrayOutputStream();Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return body;}
        public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
        public void onNext(List<ByteBuffer> items){for(var item:items){if(bytes.size()+item.remaining()>131072){subscription.cancel();body.completeExceptionally(new IOException("AI response limit"));return;}byte[] b=new byte[item.remaining()];item.get(b);bytes.writeBytes(b);}subscription.request(1);}
        public void onError(Throwable t){body.completeExceptionally(t);}
        public void onComplete(){body.complete(bytes.toByteArray());}
    }
}
