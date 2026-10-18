package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.DeviceRepository;
import com.taktak.app.data.session.Session;
import com.taktak.app.data.session.SessionStore;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DeviceRepositoryTest {
    private static final class Memory implements SessionStore {
        private Session session;
        public Session load() { return session; }
        public void save(Session session) { this.session = session; }
        public void clear() { session = null; }
    }

    private void login(ApiClient client) throws Exception {
        var pair = new TokenResponse();
        pair.accessToken = "header." + UUID.randomUUID().toString().replace("-", "") + ".signature";
        pair.refreshToken = UUID.randomUUID().toString().replace("-", "") + "abcdefghijk";
        pair.expiresIn = 900;
        pair.tokenType = "Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(), pair);
    }

    @Test
    public void registerSendsPostWithDeviceIdAndFcmToken() throws Exception {
        String testDeviceId = "test-device-uuid-123";
        String testToken = "test-fcm-token-xyz";

        try (var server = new TestHttpServer(request -> {
            assertEquals("/api/devices", request.path);
            assertNotNull(request.authorization);
            assertTrue(request.body.contains("\"deviceId\":\"" + testDeviceId + "\""));
            assertTrue(request.body.contains("\"fcmToken\":\"" + testToken + "\""));
            return new TestHttpServer.Reply(204, "");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory());
            login(client);
            var repo = new DeviceRepository(client, () -> testDeviceId);
            repo.register(testToken);
        }
    }

    @Test
    public void unregisterSendsDeleteWithDeviceId() throws Exception {
        String testDeviceId = "test-device-uuid-456";

        try (var server = new TestHttpServer(request -> {
            assertEquals("/api/devices/" + testDeviceId, request.path);
            assertNotNull(request.authorization);
            return new TestHttpServer.Reply(204, "");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory());
            login(client);
            var repo = new DeviceRepository(client, () -> testDeviceId);
            repo.unregister();
        }
    }

    @Test
    public void blankFcmTokenDoesNotTriggerNetworkCall() throws Exception {
        var calls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            calls.incrementAndGet();
            return new TestHttpServer.Reply(204, "");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory());
            login(client);
            var repo = new DeviceRepository(client, () -> "dev-1");
            repo.register(null);
            repo.register("  ");
            assertEquals(0, calls.get());
        }
    }

    @Test
    public void serverErrorThrowsApiFailure() throws Exception {
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(400, "{\"status\":400,\"code\":\"DEVICE_ID_REQUIRED\"}"))) {
            var client = new ApiClient(server.baseUrl(), new Memory());
            login(client);
            var repo = new DeviceRepository(client, () -> "dev-bad");
            try {
                repo.register("token-valid");
                fail("Should throw ApiFailure on 400 error");
            } catch (ApiFailure failure) {
                assertEquals(ApiFailure.Kind.VALIDATION, failure.kind);
                assertEquals(400, failure.status);
            }
        }
    }
}
