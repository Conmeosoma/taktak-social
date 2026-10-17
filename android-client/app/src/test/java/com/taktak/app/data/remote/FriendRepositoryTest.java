package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.FriendRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class FriendRepositoryTest {
    private static final String ID = "00000000-0000-0000-0000-000000000002";
    private static final String USER = "{\"id\":\"" + ID + "\",\"username\":\"friend_1\",\"displayName\":\"Friend\"}";
    private static final String INVITE = "{\"id\":\"00000000-0000-0000-0000-000000000003\",\"sender\":" + USER
            + ",\"receiverId\":\"00000000-0000-0000-0000-000000000001\",\"status\":\"pending\",\"createdAt\":\"2026-10-17T00:00:00Z\"}";
    private static class Memory implements SessionStore {
        Session value;
        public Session load() { return value; }
        public void save(Session next) { value = next; }
        public void clear() { value = null; }
    }
    private void seed(ApiClient client) throws Exception {
        TokenResponse tokens = new TokenResponse();
        tokens.accessToken = "header." + UUID.randomUUID().toString().replace("-", "") + ".signature";
        tokens.refreshToken = UUID.randomUUID().toString().replace("-", "") + "abcdefghijk";
        tokens.expiresIn = 900; tokens.tokenType = "Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(), tokens);
    }
    private void failure(ApiFailure.Kind kind, Callable<?> work) throws Exception {
        try { work.call(); fail("Expected typed failure"); }
        catch (ApiFailure actual) { assertEquals(kind, actual.kind); }
    }
    @Test public void queryNormalizationMatchesServerWithoutWildcardOrLocaleLeak() {
        assertEquals("a_b", FriendRepository.normalizeQuery("\u2003A_B\u2003"));
        assertEquals("a", FriendRepository.normalizeQuery("A"));
        for (String bad : new String[]{"", " ", "%", "a b", "a/b", "á", "a".repeat(256)}) {
            assertNull(FriendRepository.normalizeQuery(bad));
        }
        assertNull(FriendRepository.normalizeQuery(null));
    }
    @Test public void searchAndBothPagesUseProtectedTransportAndValidateContract() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (request.authorization == null) { return new TestHttpServer.Reply(401, "{}"); }
            requests.incrementAndGet();
            if (request.path.equals("/api/users/search?q=friend_1")) { return new TestHttpServer.Reply(200, "[" + USER + "]"); }
            String item = request.path.contains("requests") ? INVITE : USER;
            return new TestHttpServer.Reply(200, "{\"items\":[" + item + "],\"page\":1,\"hasNext\":true}");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
            var repo = new FriendRepository(client);
            assertEquals(ID, repo.search(" FRIEND_1 ").get(0).id);
            assertTrue(repo.incoming(1).hasNext);
            assertEquals(1, repo.friends(1).page);
            assertEquals(3, requests.get());
        }
    }
    @Test public void mutationsSerializeIdsAndActionsAndAccept204() throws Exception {
        try (var server = new TestHttpServer(request -> {
            assertNotNull(request.authorization);
            if (request.path.equals("/api/friends/requests")) {
                assertEquals("{\"receiverId\":\"" + ID + "\"}", request.body);
                return new TestHttpServer.Reply(201, INVITE);
            }
            if (request.path.contains("requests/")) {
                boolean accept = request.body.equals("{\"action\":\"accept\"}");
                assertTrue(accept || request.body.equals("{\"action\":\"reject\"}"));
                return new TestHttpServer.Reply(200, INVITE.replace("pending", accept ? "accepted" : "rejected"));
            }
            return new TestHttpServer.Reply(204, "");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
            var repo = new FriendRepository(client);
            assertEquals("pending", repo.send(ID).status);
            assertEquals("accepted", repo.respond(ID, true).status);
            assertEquals("rejected", repo.respond(ID, false).status);
            repo.unfriend(ID); repo.block(ID);
        }
    }
    @Test public void invalidInputsNeverReachTransport() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { requests.incrementAndGet(); return new TestHttpServer.Reply(200, "[]"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new FriendRepository(client);
            failure(ApiFailure.Kind.VALIDATION, () -> repo.search("%"));
            failure(ApiFailure.Kind.VALIDATION, () -> repo.send("../auth"));
            failure(ApiFailure.Kind.VALIDATION, () -> repo.friends(-1));
            failure(ApiFailure.Kind.VALIDATION, () -> repo.incoming(10001));
            assertEquals(0, requests.get());
        }
    }
    @Test public void malformedSuccessfulBodiesAreRejected() throws Exception {
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(200,
                request.path.contains("search") ? "[null]" : "{\"items\":[],\"page\":9}"))) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new FriendRepository(client);
            failure(ApiFailure.Kind.SERVER, () -> repo.search("a"));
            failure(ApiFailure.Kind.SERVER, () -> repo.friends(0));
            failure(ApiFailure.Kind.SERVER, () -> repo.incoming(0));
        }
    }
    @Test public void conflictNotFoundAndRetryAfterRemainTypedWithoutLeakingBody() throws Exception {
        for (int status : new int[]{409, 404, 429}) {
            try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(status,
                    "{\"status\":" + status + ",\"code\":\"FRIEND_TARGET_UNAVAILABLE\",\"message\":\"private upstream text\"}", "Retry-After: 2\r\n"))) {
                var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
                try { new FriendRepository(client).search("a"); fail(); }
                catch (ApiFailure failure) {
                    assertEquals(status, failure.status);
                    assertFalse(failure.getMessage().contains("private upstream"));
                    if (status == 429) { assertEquals(2, failure.retryAfterSeconds); }
                }
            }
        }
    }
    @Test public void repositoryCannotBeReusedAfterAccountSwitch() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { requests.incrementAndGet(); return new TestHttpServer.Reply(200, "[]"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new FriendRepository(client);
            seed(client);
            failure(ApiFailure.Kind.CANCELLED, () -> repo.search("a"));
            failure(ApiFailure.Kind.CANCELLED, () -> repo.send(ID));
            client.sessions.beginLogin();
            failure(ApiFailure.Kind.UNAUTHORIZED, () -> repo.search("a"));
            assertEquals(0, requests.get());
        }
    }
    @Test public void lateResponseFromOldAccountCannotEscapeRepository() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try (var server = new TestHttpServer(request -> {
            entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); return new TestHttpServer.Reply(200, "[" + USER + "]");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new FriendRepository(client);
            var future = executor.submit(() -> repo.search("a"));
            assertTrue(entered.await(5, TimeUnit.SECONDS)); seed(client); release.countDown();
            try { future.get(5, TimeUnit.SECONDS); fail(); }
            catch (ExecutionException failure) { assertEquals(ApiFailure.Kind.CANCELLED, ((ApiFailure) failure.getCause()).kind); }
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
