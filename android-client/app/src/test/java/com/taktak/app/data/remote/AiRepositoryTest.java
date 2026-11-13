package com.taktak.app.data.remote;

import com.taktak.app.data.model.AiModels.Send;
import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.AiRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiRepositoryTest {
    private static final String ROOM = "00000000-0000-4000-8000-000000000001";
    private static final String KEY = "00000000-0000-4000-8000-000000000002";
    private static final String MESSAGE = "{\"id\":\"00000000-0000-4000-8000-000000000003\","
            + "\"conversationId\":\"" + ROOM + "\",\"role\":\"assistant\",\"content\":\"Hello\","
            + "\"createdAt\":\"2026-11-13T00:00:00Z\",\"inputTokens\":3,\"outputTokens\":2}";
    private static final String CONVERSATION = "{\"id\":\"" + ROOM + "\",\"title\":\"Taktak AI\","
            + "\"createdAt\":\"2026-11-13T00:00:00Z\",\"updatedAt\":\"2026-11-13T00:00:00Z\"}";
    private static class Memory implements SessionStore {
        Session value;
        public Session load() { return value; }
        public void save(Session next) { value = next; }
        public void clear() { value = null; }
    }
    private static void seed(ApiClient client) throws Exception {
        var tokens = new TokenResponse(); tokens.accessToken = "qa." + UUID.randomUUID() + ".signature";
        tokens.refreshToken = UUID.randomUUID().toString().replace("-", "") + "abcdefghijk";
        tokens.expiresIn = 900; tokens.tokenType = "Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(), tokens);
    }
    private static ApiFailure fails(ApiFailure.Kind kind, Callable<?> action) throws Exception {
        try { action.call(); fail("Expected typed failure"); }
        catch (ApiFailure error) { assertEquals(kind, error.kind); return error; }
        throw new AssertionError();
    }
    @Test public void endpointsUseExistingJwtAndMatchContract() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            assertTrue(request.authorization.startsWith("Bearer ")); calls.incrementAndGet();
            if (request.path.endsWith("/status")) return new TestHttpServer.Reply(200,
                    "{\"availability\":\"CONFIGURED\",\"privacyNotice\":\"Sent to Google\"}");
            if (request.path.endsWith("messages?page=0")) return new TestHttpServer.Reply(200,
                    "{\"items\":[" + MESSAGE + "],\"hasNext\":true}");
            if (request.path.endsWith("conversations?page=0")) return new TestHttpServer.Reply(200,
                    "{\"items\":[" + CONVERSATION + "],\"hasNext\":false}");
            if (request.path.endsWith(ROOM)) return new TestHttpServer.Reply(204, "");
            return new TestHttpServer.Reply(200, CONVERSATION);
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
            var repo = new AiRepository(client);
            assertTrue(repo.status().canSend()); assertEquals(ROOM, repo.create().id);
            assertEquals(ROOM, repo.conversations(0).items.get(0).id);
            assertTrue(repo.messages(ROOM, 0).hasNext); repo.delete(ROOM); assertEquals(5, calls.get());
        }
    }
    @Test public void retryPreservesNullConversationAndKeyWithoutAutomaticRetry() throws Exception {
        AtomicInteger sends = new AtomicInteger(); AtomicReference<String> first = new AtomicReference<>();
        try (var server = new TestHttpServer(request -> {
            if (sends.incrementAndGet() == 1) { first.set(request.body); return new TestHttpServer.Reply(503, "{}"); }
            assertEquals(first.get(), request.body);
            assertFalse(request.body.contains("conversationId")); assertTrue(request.body.contains(KEY));
            assertTrue(request.body.contains("\"shareWithGoogle\":true"));
            return new TestHttpServer.Reply(200, MESSAGE);
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new AiRepository(client);
            Send pending = new Send(null, KEY, "Hello", true);
            fails(ApiFailure.Kind.SERVER, () -> repo.send(pending)); assertEquals(1, sends.get());
            assertEquals(ROOM, repo.send(pending).conversationId); assertEquals(2, sends.get());
        }
    }
    @Test public void consentAndUtf16InputLimitsAreEnforcedBeforeNetwork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { calls.incrementAndGet(); return new TestHttpServer.Reply(200, MESSAGE); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new AiRepository(client);
            for (Send value : new Send[]{null, new Send(null, KEY, "hi", false), new Send(null, "bad", "hi", true),
                    new Send("../path", KEY, "hi", true), new Send(null, KEY, " \n", true),
                    new Send(null, KEY, "a\0b", true), new Send(null, KEY, "😀".repeat(2001), true)})
                fails(ApiFailure.Kind.VALIDATION, () -> repo.send(value));
            fails(ApiFailure.Kind.VALIDATION, () -> repo.conversations(-1));
            assertEquals(0, calls.get()); assertTrue(AiRepository.validText("😀".repeat(2000)));
        }
    }
    @Test public void foreignAndMalformedResponsesAreRejected() throws Exception {
        for (String body : new String[]{"null", MESSAGE.replace(ROOM, KEY), MESSAGE.replace("assistant", "tool"),
                MESSAGE.replace("Hello", ""), MESSAGE.replace("2026-11-13T00:00:00Z", "not-time")}) {
            try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(200, body))) {
                var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
                fails(ApiFailure.Kind.SERVER, () -> new AiRepository(client).send(new Send(ROOM, KEY, "hi", true)));
            }
        }
    }
    @Test public void rateLimitDoesNotRetryAndPreservesSession() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            calls.incrementAndGet(); return new TestHttpServer.Reply(429,
                    "{\"status\":429,\"code\":\"AI_RATE_LIMITED\"}", "Retry-After: 25\r\n");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
            var error = fails(ApiFailure.Kind.RATE_LIMIT, () -> new AiRepository(client).send(new Send(null, KEY, "hi", true)));
            assertEquals(25, error.retryAfterSeconds); assertEquals("AI_RATE_LIMITED", error.code);
            assertEquals(1, calls.get()); assertNotNull(client.sessions.snapshot());
        }
    }
    @Test public void staleSessionCannotSendOrPublish() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { calls.incrementAndGet(); return new TestHttpServer.Reply(200, MESSAGE); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); seed(client); var repo = new AiRepository(client);
            seed(client); fails(ApiFailure.Kind.CANCELLED, () -> repo.send(new Send(null, KEY, "hi", true)));
            assertFalse(repo.publishIfCurrent(() -> fail("Must not publish to new account"))); assertEquals(0, calls.get());
        }
    }
    @Test public void pagesRejectMissingItemsDuplicatesAndForeignMessages() throws Exception {
        for (String body : new String[]{"{}", "{\"items\":[null]}", "{\"items\":[" + MESSAGE + "," + MESSAGE + "]}",
                "{\"items\":[" + MESSAGE.replace(ROOM, KEY) + "]}"}) {
            try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(200, body))) {
                var client = new ApiClient(server.baseUrl(), new Memory()); seed(client);
                fails(ApiFailure.Kind.SERVER, () -> new AiRepository(client).messages(ROOM, 0));
            }
        }
    }
    @Test public void payloadLoggingDoesNotRevealText() {
        assertFalse(new Send(null, KEY, "private", true).toString().contains("private"));
        assertFalse(new com.taktak.app.data.model.AiModels.Message().toString().contains("content"));
    }
}
