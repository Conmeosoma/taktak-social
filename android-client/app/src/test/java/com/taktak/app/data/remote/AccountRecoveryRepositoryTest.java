package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels;
import com.taktak.app.data.model.RecoveryModels;
import com.taktak.app.data.repository.AccountRecoveryRepository;
import com.google.gson.Gson;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import static org.junit.Assert.*;

public class AccountRecoveryRepositoryTest {
    private AccountRecoveryRepository repository(TestHttpServer server) {
        return new AccountRecoveryRepository(new Retrofit.Builder().baseUrl(server.baseUrl())
                .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi.class));
    }
    @Test public void forgotSendsOnlyEmailWithoutBearerAndUsesExpectedEndpoint() throws Exception {
        try (var server=new TestHttpServer(request -> {
            assertEquals("/api/auth/forgot-password",request.path); assertNull(request.authorization);
            var body=new Gson().fromJson(request.body,com.google.gson.JsonObject.class);
            assertEquals(1,body.size()); assertEquals("qa@example.test",body.get("email").getAsString());
            return new TestHttpServer.Reply(200,"{\"message\":\"accepted\"}");
        })) { repository(server).forgot("qa@example.test"); }
    }
    @Test public void verificationRequestRequiresOriginalCredential() throws Exception {
        try (var server=new TestHttpServer(request -> {
            assertEquals("/api/auth/verification/request",request.path); assertNull(request.authorization);
            assertTrue(request.body.contains("\"password\""));
            return new TestHttpServer.Reply(200,"{\"message\":\"accepted\"}");
        })) { repository(server).requestVerification("qa@example.test","qa-fixture-password"); }
    }
    @Test public void confirmUsesSeparateEndpointAndIncludesCode() throws Exception {
        try (var server=new TestHttpServer(request -> {
            assertEquals("/api/auth/verification/confirm",request.path);
            assertTrue(request.body.contains("\"code\""));
            return new TestHttpServer.Reply(200,"{\"message\":\"verified\"}");
        })) { repository(server).verify("qa@example.test","qa-fixture-password","a".repeat(43)); }
    }
    @Test public void failureDoesNotBecomeSuccessfulDelivery() throws Exception {
        try (var server=new TestHttpServer(request -> new TestHttpServer.Reply(503,"{\"status\":503,\"code\":\"MAIL_UNAVAILABLE\"}"))) {
            try { repository(server).requestVerification("qa@example.test","qa-fixture-password"); fail(); }
            catch (ApiFailure e) { assertEquals(ApiFailure.Kind.SERVER,e.kind); }
        }
    }
    @Test public void emptySuccessBodyIsRejected() throws Exception {
        try (var server=new TestHttpServer(request -> new TestHttpServer.Reply(200,"{}"))) {
            try { repository(server).forgot("qa@example.test"); fail(); }
            catch (ApiFailure e) { assertEquals(ApiFailure.Kind.SERVER,e.kind); }
        }
    }
    @Test public void resetResponseCannotBePersistedAsFullSessionAndModelsRedactSecrets() {
        var response=new Gson().fromJson("{\"resetRequired\":true,\"resetToken\":\""+"a".repeat(43)+"\",\"expiresIn\":900}",AuthModels.TokenResponse.class);
        assertFalse(response.isValid()); assertNull(response.accessToken); assertNull(response.refreshToken);
        response.accessToken="header.payload.signature"; response.refreshToken="b".repeat(43); response.tokenType="Bearer";
        assertFalse("Mixed reset/full-session response must not be accepted",response.isValid());
        assertEquals("Reset[redacted]",new RecoveryModels.Reset("private","temporary","password","confirm").toString());
        assertEquals("Verification[redacted]",new RecoveryModels.Verification("private","password","code").toString());
    }
    @Test public void networkFailureCanBeRetriedWithoutClaimingDelivery() throws Exception {
        var attempts=new java.util.concurrent.atomic.AtomicInteger();
        try(var server=new TestHttpServer(request -> {
            if(attempts.incrementAndGet()==1) throw new java.io.IOException("Simulated disconnect.");
            return new TestHttpServer.Reply(200,"{\"message\":\"accepted\"}");
        })) {
            var recovery=repository(server);
            try { recovery.forgot("qa@example.test"); fail("Disconnected request must not report success"); }
            catch(java.io.IOException expected) { }
            recovery.forgot("qa@example.test");
            assertEquals(2,attempts.get());
        }
    }
}
