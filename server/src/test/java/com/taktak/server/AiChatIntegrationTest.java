package com.taktak.server;
import com.taktak.server.ai.*;
import com.fasterxml.jackson.databind.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiChatIntegrationTest {
    EmbeddedPostgres pg;ServletWebServerApplicationContext app;JdbcTemplate db;Fixture provider;String base,token;ObjectMapper json=new ObjectMapper();HttpClient http=HttpClient.newHttpClient();
    @BeforeAll void start()throws Exception{
        Path root=Files.createDirectories(Path.of("target","ai-tests"));
        pg=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"db-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setPort(0).setServerConfig("listen_addresses","127.0.0.1").start();
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        app=(ServletWebServerApplicationContext)new SpringApplication(TaktakApplication.class,Beans.class).run(
                "--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
                "--server.port=0","--server.address=127.0.0.1","--logging.level.root=WARN",
                "--GEMINI_USER_RPM=1000","--GEMINI_APP_RPM=1000","--GEMINI_APP_DAILY_REQUESTS=1000","--GEMINI_CONTEXT_CHARS=4000",
                "--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key));
        base="http://127.0.0.1:"+app.getWebServer().getPort();db=app.getBean(JdbcTemplate.class);provider=app.getBean(Fixture.class);
    }
    @AfterAll void close()throws Exception{try{if(app!=null)app.close();}finally{if(pg!=null)pg.close();}}
    @BeforeEach void reset()throws Exception{db.execute("truncate users cascade");provider.calls.set(0);provider.fail=false;provider.block=false;provider.availability="CONFIGURED";token=account();}
    String account()throws Exception{
        String name="ai_"+UUID.randomUUID().toString().replace("-","");String password=UUID.randomUUID().toString();
        assertThat(call("POST","/api/auth/register",Map.of("email",name+"@example.test","username",name,"displayName","QA","password",password),null).statusCode()).isEqualTo(201);
        return json.readTree(call("POST","/api/auth/login",Map.of("email",name+"@example.test","password",password),null).body()).get("accessToken").asText();
    }
    HttpResponse<String> call(String method,String path,Object body,String jwt)throws Exception{
        var req=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");if(jwt!=null)req.header("Authorization","Bearer "+jwt);
        return http.send(req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    Map<String,Object> request(UUID id,String message){var body=new HashMap<String,Object>();body.put("clientRequestId",UUID.randomUUID());body.put("conversationId",id);body.put("message",message);body.put("shareWithGoogle",true);return body;}
    UUID conversation()throws Exception{return UUID.fromString(json.readTree(call("POST","/api/ai/conversations",null,token).body()).get("id").asText());}
    @Test void jwtAndOwnershipProtectEveryHistoryOperation()throws Exception{
        assertThat(call("GET","/api/ai/status",null,null).statusCode()).isEqualTo(401);
        UUID id=conversation();String other=account();
        assertThat(call("GET","/api/ai/conversations/"+id+"/messages",null,other).statusCode()).isEqualTo(404);
        assertThat(call("DELETE","/api/ai/conversations/"+id,null,other).statusCode()).isEqualTo(404);
        assertThat(call("POST","/api/ai/chat",request(id,"hello"),other).statusCode()).isEqualTo(404);assertThat(provider.calls.get()).isZero();
    }
    @Test void successPersistsHistoryAndDeleteCascades()throws Exception{
        var result=call("POST","/api/ai/chat",request(null,"Xin chào"),token);assertThat(result.statusCode()).isEqualTo(200);
        var id=json.readTree(result.body()).get("conversationId").asText();
        var history=json.readTree(call("GET","/api/ai/conversations/"+id+"/messages",null,token).body());assertThat(history.get("items").size()).isEqualTo(2);
        assertThat(call("DELETE","/api/ai/conversations/"+id,null,token).statusCode()).isEqualTo(204);
        assertThat(db.queryForObject("select count(*) from ai_messages",Integer.class)).isZero();assertThat(db.queryForObject("select count(*) from ai_requests",Integer.class)).isZero();
    }
    @Test void duplicateReplaysWithoutSpendingAgainAndChangedPayloadConflicts()throws Exception{
        var body=request(null,"hello");var first=call("POST","/api/ai/chat",body,token);assertThat(first.statusCode()).isEqualTo(200);
        assertThat(call("POST","/api/ai/chat",body,token).body()).isEqualTo(first.body());assertThat(provider.calls.get()).isEqualTo(1);
        body.put("message","different");assertThat(call("POST","/api/ai/chat",body,token).statusCode()).isEqualTo(409);
    }
    @Test void failureRollsBackConversationAndDoesNotSaveFakeAnswer()throws Exception{
        provider.fail=true;assertThat(call("POST","/api/ai/chat",request(null,"hello"),token).statusCode()).isEqualTo(503);
        assertThat(db.queryForObject("select count(*) from ai_messages",Integer.class)).isZero();assertThat(db.queryForObject("select count(*) from ai_conversations",Integer.class)).isZero();
    }
    @Test void validationConsentBodyLimitAndMissingConversation()throws Exception{
        for(String input:List.of(""," ","a".repeat(4001)))assertThat(call("POST","/api/ai/chat",request(null,input),token).statusCode()).isEqualTo(400);
        var body=request(null,"hello");body.put("shareWithGoogle",false);assertThat(call("POST","/api/ai/chat",body,token).statusCode()).isEqualTo(400);
        assertThat(call("POST","/api/ai/chat",request(null,"a".repeat(25000)),token).statusCode()).isEqualTo(413);
        assertThat(call("POST","/api/ai/chat",request(UUID.randomUUID(),"hi"),token).statusCode()).isEqualTo(404);assertThat(provider.calls.get()).isZero();
    }
    @Test void contextAndPaginationAreBounded()throws Exception{
        UUID id=conversation();for(int i=0;i<11;i++)assertThat(call("POST","/api/ai/chat",request(id,"x".repeat(1800)),token).statusCode()).isEqualTo(200);
        assertThat(provider.context.stream().mapToInt(t->t.text().length()).sum()).isLessThanOrEqualTo(4000);
        assertThat(provider.context.get(0).role()).isEqualTo("user");
        var history=json.readTree(call("GET","/api/ai/conversations/"+id+"/messages",null,token).body());assertThat(history.get("items").size()).isEqualTo(20);assertThat(history.get("hasNext").asBoolean()).isTrue();
        assertThat(call("GET","/api/ai/conversations?page=-1",null,token).statusCode()).isEqualTo(400);
    }
    @Test void concurrentSameRequestNeverGeneratesTwice()throws Exception{
        UUID id=conversation();provider.block=true;provider.entered=new CountDownLatch(1);provider.release=new CountDownLatch(1);var body=request(id,"hello");
        var pool=Executors.newSingleThreadExecutor();
        try{
            var first=pool.submit(()->call("POST","/api/ai/chat",body,token));assertThat(provider.entered.await(5,TimeUnit.SECONDS)).isTrue();
            assertThat(call("POST","/api/ai/chat",body,token).statusCode()).isEqualTo(409);
            assertThat(call("DELETE","/api/ai/conversations/"+id,null,token).statusCode()).isEqualTo(409);
            provider.release.countDown();assertThat(first.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(call("POST","/api/ai/chat",body,token).statusCode()).isEqualTo(200);assertThat(provider.calls.get()).isEqualTo(1);
        }finally{provider.release.countDown();pool.shutdownNow();}
    }
    @Test void disabledProviderDoesNotAffectExistingAuth()throws Exception{
        provider.availability="DISABLED";assertThat(call("POST","/api/ai/chat",request(null,"hello"),token).statusCode()).isEqualTo(503);
        assertThat(call("GET","/api/users/me",null,token).statusCode()).isEqualTo(200);assertThat(provider.calls.get()).isZero();
    }
    @Test void fullConversationRejectsBeforeSpendingOnGeneration()throws Exception{
        UUID id=conversation();
        db.update("insert into ai_messages(id,conversation_id,client_request_id,role,content) select gen_random_uuid(),?,gen_random_uuid(),'user','QA' from generate_series(1,1000)",id);
        assertThat(call("POST","/api/ai/chat",request(id,"hello"),token).statusCode()).isEqualTo(409);
        assertThat(provider.calls.get()).isZero();
    }
    @TestConfiguration static class Beans{@Bean @Primary Fixture fixture(){return new Fixture();}}
    static class Fixture implements AiProvider{
        AtomicInteger calls=new AtomicInteger();volatile boolean fail,block;volatile String availability="CONFIGURED";List<Turn> context;CountDownLatch entered,release;
        public String availability(){return availability;}
        public Answer generate(List<Turn> context){this.context=context;calls.incrementAndGet();if(fail)throw new AiFailure("AI_TEMPORARILY_UNAVAILABLE",org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
            if(block){entered.countDown();try{release.await(8,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException();}}
            return new Answer("QA answer",3,2);
        }
    }
}
