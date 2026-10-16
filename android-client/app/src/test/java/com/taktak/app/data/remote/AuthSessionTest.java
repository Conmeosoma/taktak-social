package com.taktak.app.data.remote;

import com.google.gson.Gson;
import com.taktak.app.data.model.AuthModels.*;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Request;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthSessionTest {
    private static final Gson JSON = new Gson();
    private String access() { return "eyJhbGciOiJIUzI1NiJ9." + java.util.UUID.randomUUID().toString().replace("-", "") + ".signature"; }
    private TokenResponse tokens() {
        TokenResponse pair = new TokenResponse(); pair.accessToken = access();
        pair.refreshToken = java.util.UUID.randomUUID().toString().replace("-", "") + "abcdefghijk";
        pair.expiresIn = 900; pair.tokenType = "Bearer"; return pair;
    }
    private String profile() {
        return "{\"id\":\"" + java.util.UUID.randomUUID() + "\",\"email\":\"member@example.test\",\"username\":\"member\",\"displayName\":\"Tiến 📷\",\"createdAt\":\"2026-10-16T00:00:00Z\"}";
    }
    private void seed(ApiClient client) throws IOException { client.sessions.completeLogin(client.sessions.beginLogin(), tokens()); }
    private static final class MemoryStore implements SessionStore {
        Session session;
        boolean failWrite;
        @Override public Session load() { return session; }
        @Override public void save(Session next) throws IOException { if (failWrite) { throw new IOException("Test storage failure."); } session = next; }
        @Override public void clear() { session = null; }
    }

    @Test public void loginStoresBothTokensAndProfileUsesBearerButAuthDoesNot() throws Exception {
        var pair = tokens();
        AtomicInteger profileCalls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.path.equals("/api/auth/login")) {
                assertNull(request.authorization);
                return new TestHttpServer.Reply(200, JSON.toJson(pair));
            }
            if (!("Bearer " + pair.accessToken).equals(request.authorization)) { return new TestHttpServer.Reply(401, "{}"); }
            profileCalls.incrementAndGet(); return new TestHttpServer.Reply(200, profile());
        })) {
            MemoryStore store = new MemoryStore();
            ApiClient client = new ApiClient(server.baseUrl(), store);
            assertNotNull(new AuthRepository(client).login("member@example.test", java.util.UUID.randomUUID().toString()).id);
            assertTrue(pair.refreshToken.equals(store.session.refreshToken));
            assertEquals(1, profileCalls.get());
            assertFalse(store.session.toString().contains(pair.refreshToken));
            assertFalse(pair.toString().contains(pair.accessToken));
        }
    }

    @Test public void concurrent401RefreshesExactlyOnceAndAllRequestsRetrySuccessfully() throws Exception {
        AtomicInteger rotations = new AtomicInteger();
        CountDownLatch initialRequests = new CountDownLatch(6);
        TokenResponse next = tokens();
        try (var server = new TestHttpServer(request -> {
            if (request.path.equals("/api/auth/refresh")) {
                assertNull(request.authorization); rotations.incrementAndGet(); return new TestHttpServer.Reply(200, JSON.toJson(next));
            }
            if (("Bearer " + next.accessToken).equals(request.authorization)) { return new TestHttpServer.Reply(200, profile()); }
            initialRequests.countDown();
            if (!initialRequests.await(10, TimeUnit.SECONDS)) { throw new IOException("401 barrier timed out."); }
            return new TestHttpServer.Reply(401, "{}");
        })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            var executor = Executors.newFixedThreadPool(6);
            try {
                var calls = java.util.stream.IntStream.range(0, 6).mapToObj(index -> executor.submit(() -> new AuthRepository(client).profile())).toList();
                for (var call : calls) { assertNotNull(call.get(20, TimeUnit.SECONDS).id); }
                assertEquals(1, rotations.get());
                assertTrue(next.refreshToken.equals(client.sessions.snapshot().refreshToken));
            } finally { executor.shutdownNow(); }
        }
    }

    @Test public void rejectedRefreshClearsSessionWithoutRecursion() throws Exception {
        AtomicInteger rotations = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.path.equals("/api/auth/refresh")) { rotations.incrementAndGet(); }
            return new TestHttpServer.Reply(401, "{\"status\":401,\"code\":\"UNAUTHORIZED\"}");
        })) {
            MemoryStore store = new MemoryStore(); ApiClient client = new ApiClient(server.baseUrl(), store); seed(client);
            try { new AuthRepository(client).profile(); fail(); } catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.UNAUTHORIZED, failure.kind); }
            assertNull(client.sessions.snapshot()); assertNull(store.session); assertEquals(1, rotations.get());
        }
    }

    @Test public void second401StopsAfterOneRetryAndClearsTheCurrentSession() throws Exception {
        AtomicInteger refresh = new AtomicInteger(); AtomicInteger profile = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.path.equals("/api/auth/refresh")) { refresh.incrementAndGet(); return new TestHttpServer.Reply(200, JSON.toJson(tokens())); }
            profile.incrementAndGet(); return new TestHttpServer.Reply(401, "{}");
        })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            try { new AuthRepository(client).profile(); fail(); } catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.UNAUTHORIZED, failure.kind); }
            assertEquals(1, refresh.get()); assertEquals(2, profile.get()); assertNull(client.sessions.snapshot());
        }
    }

    @Test public void transientRefreshFailureIsCoalescedAndKeepsSessionForManualRetry() throws Exception {
        AtomicInteger refresh = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.path.equals("/api/auth/refresh")) { refresh.incrementAndGet(); return new TestHttpServer.Reply(503, "{}"); }
            return new TestHttpServer.Reply(401, "{}");
        })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            for (int index = 0; index < 4; index++) {
                try { new AuthRepository(client).profile(); fail(); } catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.SERVER, failure.kind); }
            }
            assertEquals(1, refresh.get()); assertNotNull(client.sessions.snapshot());
        }
    }

    @Test public void lateLoginCannotRestoreSessionAfterClearOrNewLogin() throws Exception {
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(500, "{}"))) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore());
            long old = client.sessions.beginLogin(); client.sessions.clear();
            try { client.sessions.completeLogin(old, tokens()); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.CANCELLED, failure.kind); }
            long latest = client.sessions.beginLogin(); client.sessions.completeLogin(latest, tokens());
            Session current = client.sessions.snapshot();
            assertNull(client.sessions.refresh(new Session(access(), tokens().refreshToken, 0, old)));
            assertSame(current, client.sessions.snapshot());
        }
    }

    @Test public void credentialsNeverAttachToAnotherOriginOrAuthPathAndRedirectIsNotFollowed() throws Exception {
        AtomicInteger leaked = new AtomicInteger();
        try (var other = new TestHttpServer(request -> { if (request.authorization != null) { leaked.incrementAndGet(); } return new TestHttpServer.Reply(200, "{}"); });
             var server = new TestHttpServer(request -> {
                 if (request.path.equals("/api/auth/login")) { if (request.authorization != null) { leaked.incrementAndGet(); } return new TestHttpServer.Reply(401, "{}"); }
                 return new TestHttpServer.Reply(302, "", "Location: " + other.baseUrl() + "api/users/me\r\n");
             })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            try (var response = client.execute(new Request.Builder().url(other.baseUrl() + "api/users/me").header("Authorization", "must-be-removed").build())) { assertEquals(200, response.code()); }
            try (var response = client.execute(new Request.Builder().url(server.baseUrl() + "api/auth/login").build())) { assertEquals(401, response.code()); }
            try (var response = client.execute(new Request.Builder().url(server.baseUrl() + "api/users/me").build())) { assertEquals(302, response.code()); }
            assertEquals(0, leaked.get());
        }
    }

    @Test public void errorCodesAndRetryAfterAreParsedWithoutExposingRemoteMessages() throws Exception {
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(429,
                "{\"status\":429,\"code\":\"LOGIN_RATE_LIMITED\",\"message\":\"fixture-sensitive-value\"}", "Retry-After: 120\r\n"))) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore());
            try { new AuthRepository(client).login("member@example.test", java.util.UUID.randomUUID().toString()); fail(); }
            catch (ApiFailure failure) {
                assertEquals(ApiFailure.Kind.RATE_LIMIT, failure.kind); assertEquals(120, failure.retryAfterSeconds);
                assertEquals("LOGIN_RATE_LIMITED", failure.code); assertFalse(failure.toString().contains("fixture-sensitive-value"));
            }
        }
    }

    @Test public void badTokenResponseOrStorageFailureNeverCreatesInMemorySession() throws Exception {
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(200, "{}"))) {
            MemoryStore store = new MemoryStore(); ApiClient client = new ApiClient(server.baseUrl(), store);
            try { client.sessions.completeLogin(client.sessions.beginLogin(), new TokenResponse()); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.SERVER, failure.kind); }
            store.failWrite = true;
            try { client.sessions.completeLogin(client.sessions.beginLogin(), tokens()); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.STORAGE, failure.kind); }
            assertNull(client.sessions.snapshot());
        }
    }

    @Test public void logoutRevokesLatestRefreshAfterRotationAndClearsEvenWhenServerFails() throws Exception {
        TokenResponse rotated = tokens(); AtomicInteger correct = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.path.endsWith("refresh")) { return new TestHttpServer.Reply(200, JSON.toJson(rotated)); }
            if (request.body.contains(rotated.refreshToken)) { correct.incrementAndGet(); }
            return new TestHttpServer.Reply(503, "{}");
        })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            client.sessions.refresh(client.sessions.snapshot());
            try { client.sessions.logout(); fail(); } catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.SERVER, failure.kind); }
            assertEquals(1, correct.get()); assertNull(client.sessions.snapshot());
        }
    }

    @Test public void delayedProtectedResponseCannotReturnOldAccountAfterSessionWasCleared() throws Exception {
        CountDownLatch received = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
        try (var server = new TestHttpServer(request -> {
            received.countDown(); if (!release.await(10, TimeUnit.SECONDS)) { throw new IOException("Response latch timed out."); }
            return new TestHttpServer.Reply(200, profile());
        })) {
            ApiClient client = new ApiClient(server.baseUrl(), new MemoryStore()); seed(client);
            var executor = Executors.newSingleThreadExecutor();
            try {
                var pending = executor.submit(() -> {
                    try { new AuthRepository(client).profile(); return false; }
                    catch (ApiFailure failure) { return failure.kind == ApiFailure.Kind.CANCELLED; }
                });
                assertTrue(received.await(5, TimeUnit.SECONDS)); client.sessions.clear(); release.countDown();
                assertTrue(pending.get(10, TimeUnit.SECONDS)); assertNull(client.sessions.snapshot());
            } finally { release.countDown(); executor.shutdownNow(); }
        }
    }

    @Test public void unreachableBackendDoesNotEraseTheExistingSession() throws Exception {
        int port;
        try (var unused = new java.net.ServerSocket(0)) { port = unused.getLocalPort(); }
        ApiClient client = new ApiClient("http://127.0.0.1:" + port + "/", new MemoryStore()); seed(client);
        Session before = client.sessions.snapshot();
        try { new AuthRepository(client).profile(); fail(); } catch (IOException expected) { }
        assertSame(before, client.sessions.snapshot());
    }
}
