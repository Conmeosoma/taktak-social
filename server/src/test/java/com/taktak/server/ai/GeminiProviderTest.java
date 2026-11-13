package com.taktak.server.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GeminiProviderTest {
    @Test void userRateBudgetIsBoundedWithoutCallingProvider(){
        var repo=mock(AiRepository.class);var ai=mock(AiProvider.class);var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        var settings=new AiSettings(true,"fixture","",128,10,0,1,1,10,10,4000);
        var service=new AiChatService(repo,ai,settings,manager,Clock.systemUTC());UUID user=UUID.randomUUID();
        service.create(user);assertThatThrownBy(()->service.create(user)).isInstanceOfSatisfying(AiFailure.class,e->assertThat(e.code).isEqualTo("AI_RATE_LIMITED"));
        verifyNoInteractions(ai);
    }
    @Test void disabledAndMissingConfigDoNotCallNetwork(){
        var http=mock(HttpClient.class);var off=new AiSettings(false,"","",128,10,0,1,1,1,1,4000);
        var provider=new GeminiProvider(off,new GeminiKeyManager(Clock.systemUTC()),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThat(provider.availability()).isEqualTo("DISABLED");assertThatThrownBy(()->provider.generate(List.of())).isInstanceOf(AiFailure.class);verifyNoInteractions(http);
        assertThat(new GeminiProvider(settings(0),new GeminiKeyManager(Clock.systemUTC()),new ObjectMapper(),new SimpleMeterRegistry(),http).availability()).isEqualTo("CONFIGURATION_ERROR");
    }
    static AiSettings settings(int retries){return new AiSettings(true,"fixture-model","",128,10,retries,2,50,100,1000,8000);}
    GeminiKeyManager.Key key(String group){return new GeminiKeyManager.Key(UUID.randomUUID().toString(),group,UUID.randomUUID().toString(),true);}
    @Test void quotaGroupBlocksAllKeysButIndependentGroupRemainsEligible(){
        var a=key("same");var b=key("same");var c=key("backup");var pool=new GeminiKeyManager(Clock.systemUTC(),a,b,c);
        pool.fail(a,"RATE",60,true,false);assertThat(pool.select()).isSameAs(c);
        pool.fail(c,"RATE",60,true,false);assertThatThrownBy(pool::select).isInstanceOf(AiFailure.class);
    }
    @Test void disabledInvalidAndCooldownAreNotSelected(){
        var a=key("g");a.invalid=true;var b=key("g");b.cooldown=Instant.now().plusSeconds(60);
        var disabled=new GeminiKeyManager.Key("d","g",UUID.randomUUID().toString(),false);
        assertThatThrownBy(()->new GeminiKeyManager(Clock.systemUTC(),a,b,disabled).select()).isInstanceOf(AiFailure.class);
    }
    @Test void classifierDistinguishesQuotaAndNeverRetriesPermissionOrPayload(){
        assertThat(GeminiErrorClassifier.classify(429,"per_day",5).category()).isEqualTo("DAILY");
        assertThat(GeminiErrorClassifier.classify(429,"billing",5).cooldown()).isEqualTo(3600);
        assertThat(GeminiErrorClassifier.classify(429,"tokens per minute",120).cooldown()).isEqualTo(120);
        assertThat(GeminiErrorClassifier.classify(503,"",3).retry()).isTrue();
        assertThat(GeminiErrorClassifier.classify(403,"permission denied",0).group()).isTrue();
        assertThat(GeminiErrorClassifier.classify(401,"API_KEY_INVALID",0).invalid()).isTrue();
        assertThat(GeminiErrorClassifier.classify(400,"",0).retry()).isFalse();
        assertThat(GeminiProvider.retryAfter("120")).isEqualTo(120);
        assertThat(GeminiProvider.retryAfter("garbage")).isZero();
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    HttpResponse<byte[]> response(int code,String body){var response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(code);when(response.body()).thenReturn(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));when(response.headers()).thenReturn(HttpHeaders.of(Map.of(),(a,b)->true));return response;}
    static final String ANSWER="{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"Xin chào\"}]}}],\"usageMetadata\":{\"promptTokenCount\":2,\"candidatesTokenCount\":3}}";
    @Test void validResponseIsSanitizedAndKeyNeverInUri()throws Exception{
        var a=key("g");var http=mock(HttpClient.class);
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->{HttpRequest request=inv.getArgument(0);assertThat(request.uri().toString()).doesNotContain(a.secret);return response(200,ANSWER);});
        var provider=new GeminiProvider(settings(0),new GeminiKeyManager(Clock.systemUTC(),a),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThat(provider.generate(List.of(new AiProvider.Turn("user","hello"))).text()).isEqualTo("Xin chào");
        assertThat(a.lastSuccess).isNotNull();
    }
    @Test void rateLimitDoesNotTryAnotherKeyInSameProject()throws Exception{
        var http=mock(HttpClient.class);when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->response(429,"daily per_day"));
        var a=key("g");var b=key("g");var pool=new GeminiKeyManager(Clock.systemUTC(),a,b);
        var provider=new GeminiProvider(settings(2),pool,new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThatThrownBy(()->provider.generate(List.of())).isInstanceOfSatisfying(AiFailure.class,e->assertThat(e.code).isEqualTo("AI_QUOTA_EXHAUSTED"));
        verify(http,times(1)).send(any(),any(HttpResponse.BodyHandler.class));assertThatThrownBy(pool::select).isInstanceOf(AiFailure.class);
    }
    @Test void unavailableRetriesBoundedlyAndDoesNotInvalidateKey()throws Exception{
        var http=mock(HttpClient.class);when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->response(503,"unavailable"));
        var a=key("g");var provider=new GeminiProvider(settings(1),new GeminiKeyManager(Clock.systemUTC(),a),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThatThrownBy(()->provider.generate(List.of())).isInstanceOf(AiFailure.class);
        verify(http,times(2)).send(any(),any(HttpResponse.BodyHandler.class));assertThat(a.invalid).isFalse();
    }
    @Test void timeoutDoesNotReplayAmbiguousGeneration()throws Exception{
        var http=mock(HttpClient.class);when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("fixture"));
        var provider=new GeminiProvider(settings(2),new GeminiKeyManager(Clock.systemUTC(),key("g")),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThatThrownBy(()->provider.generate(List.of())).isInstanceOf(AiFailure.class);verify(http,times(1)).send(any(),any(HttpResponse.BodyHandler.class));
    }
    @Test void invalidCredentialCanFailOverButPermissionCannot()throws Exception{
        var http=mock(HttpClient.class);var count=new java.util.concurrent.atomic.AtomicInteger();when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->count.getAndIncrement()==0?response(401,"API_KEY_INVALID"):response(200,ANSWER));
        var a=key("g");var b=key("g");var provider=new GeminiProvider(settings(1),new GeminiKeyManager(Clock.systemUTC(),a,b),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThat(provider.generate(List.of()).text()).isEqualTo("Xin chào");assertThat(a.invalid).isTrue();
    }
    @Test void blockedContentIsNotStoredAsFakeAnswer()throws Exception{
        var http=mock(HttpClient.class);when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->response(200,"{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"));
        var provider=new GeminiProvider(settings(0),new GeminiKeyManager(Clock.systemUTC(),key("g")),new ObjectMapper(),new SimpleMeterRegistry(),http);
        assertThatThrownBy(()->provider.generate(List.of())).isInstanceOfSatisfying(AiFailure.class,e->assertThat(e.code).isEqualTo("AI_CONTENT_UNAVAILABLE"));
    }
    @Test void oversizedResponseCancelsSubscription(){
        var body=new GeminiProvider.LimitedBody();var sub=mock(java.util.concurrent.Flow.Subscription.class);body.onSubscribe(sub);
        body.onNext(List.of(java.nio.ByteBuffer.allocate(131073)));verify(sub).cancel();assertThat(body.body.isCompletedExceptionally()).isTrue();
    }
}
