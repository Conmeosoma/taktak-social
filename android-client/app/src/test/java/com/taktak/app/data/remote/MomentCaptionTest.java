package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.MomentRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class MomentCaptionTest {
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
        pair.expiresIn = 900; pair.tokenType = "Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(), pair);
    }
    @Test public void unicodeCaptionUsesUtf8MultipartAndExplicitEmptyRecipients() throws Exception {
        String caption = new String(Character.toChars(0x1F642)).repeat(50), id = UUID.randomUUID().toString();
        try (var server = new TestHttpServer(request -> {
            assertNotNull(request.authorization); assertEquals("/api/moments", request.path);
            assertTrue(request.body.contains("name=\"caption\"")); assertTrue(request.body.contains(caption));
            assertTrue(request.body.contains("name=\"recipientIds\"")); assertTrue(request.body.contains("[]"));
            return new TestHttpServer.Reply(201, "{\"id\":\"" + id + "\"}");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            assertEquals(id, new MomentRepository(client).upload(new byte[]{1,2,3}, caption));
        }
    }
    @Test public void invalidCaptionAndChangedSessionNeverUpload() throws Exception {
        var requests = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { requests.incrementAndGet(); return new TestHttpServer.Reply(500, "{}"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client); var repository = new MomentRepository(client);
            for (String bad : new String[]{null, "a".repeat(51), "bad\0caption"}) {
                try { repository.upload(new byte[]{1,2,3}, bad); fail(); }
                catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.VALIDATION, failure.kind); }
            }
            login(client);
            try { repository.upload(new byte[]{1,2,3}, "Draft A"); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.CANCELLED, failure.kind); }
            assertEquals(0, requests.get());
        }
    }
    @Test public void selectedRecipientsUseRepeatedFieldsNotJsonOrCsv() throws Exception {
        String first = UUID.randomUUID().toString(), second = UUID.randomUUID().toString(), id = UUID.randomUUID().toString();
        try (var server = new TestHttpServer(request -> {
            assertNotNull(request.authorization);
            assertEquals(2, request.body.split("name=\"recipientIds\"", -1).length - 1);
            assertTrue(request.body.contains("\r\n\r\n" + first + "\r\n"));
            assertTrue(request.body.contains("\r\n\r\n" + second + "\r\n"));
            assertFalse(request.body.contains("[]"));
            return new TestHttpServer.Reply(201, "{\"id\":\"" + id + "\"}");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            assertEquals(id, new MomentRepository(client).upload(new byte[]{1,2,3}, "Friends", java.util.List.of(first, second)));
        }
    }
    @Test public void invalidRecipientListsNeverReachNetwork() throws Exception {
        var count = new AtomicInteger(); String id = UUID.randomUUID().toString();
        try (var server = new TestHttpServer(request -> { count.incrementAndGet(); return new TestHttpServer.Reply(500, "{}"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client); var repository = new MomentRepository(client);
            for (var ids : java.util.List.of(java.util.List.of("bad"), java.util.List.of(id, id.toUpperCase(java.util.Locale.ROOT)),
                    java.util.Collections.nCopies(101, id))) {
                try { repository.upload(new byte[]{1,2,3}, "", ids); fail(); }
                catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.VALIDATION, failure.kind); }
            }
            assertEquals(0, count.get());
        }
    }
    @Test public void imageGetRetriesTransportOnceButDoesNotRetryHttpDenial() throws Exception {
        var count = new AtomicInteger();
        try (var server = new TestHttpServer(request -> {
            if (count.incrementAndGet() == 1) { throw new java.io.IOException("Fixture closed connection"); }
            return new TestHttpServer.Reply(404,"{}");
        })) {
            var client = new ApiClient(server.baseUrl(),new Memory()); login(client);
            try { new MomentRepository(client).download(UUID.randomUUID().toString()); fail(); }
            catch (ApiFailure failure) { assertEquals(404,failure.status); }
            assertEquals(2,count.get());
            try { new MomentRepository(client).download(UUID.randomUUID().toString()); fail(); }
            catch (ApiFailure failure) { assertEquals(404,failure.status); }
            assertEquals(3,count.get());
        }
    }
    @Test public void transportRetriesAreBoundedAndPostIsNeverRetried() throws Exception {
        var count = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { count.incrementAndGet(); throw new java.io.IOException("Fixture closed connection"); })) {
            var client = new ApiClient(server.baseUrl(),new Memory()); login(client); var repository = new MomentRepository(client);
            try { repository.download(UUID.randomUUID().toString()); fail(); } catch (java.io.IOException expected) { }
            assertEquals(2,count.get());
            try { repository.upload(new byte[]{1,2,3},"",java.util.List.of(UUID.randomUUID().toString())); fail(); }
            catch (java.io.IOException expected) { }
            assertEquals(3,count.get());
        }
    }
    @Test public void idempotencyKeyIsPropagatedAndProgressInvoked() throws Exception {
        String key = UUID.randomUUID().toString(), id = UUID.randomUUID().toString();
        var progressValues = new java.util.concurrent.CopyOnWriteArrayList<Integer>();
        try (var server = new TestHttpServer(request -> {
            assertNotNull(request.authorization);
            assertEquals(key, request.headers.get("idempotency-key"));
            return new TestHttpServer.Reply(201, "{\"id\":\"" + id + "\"}");
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            String result = new MomentRepository(client).upload(new byte[]{1,2,3,4,5}, "Progress & Key", java.util.List.of(), key,
                    (percent, written, total) -> progressValues.add(percent));
            assertEquals(id, result);
            assertFalse(progressValues.isEmpty());
            assertEquals(100, (int) progressValues.get(progressValues.size() - 1));
        }
    }
    @Test public void invalidIdempotencyKeyThrowsValidationWithoutRequest() throws Exception {
        var count = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { count.incrementAndGet(); return new TestHttpServer.Reply(500, "{}"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client); var repository = new MomentRepository(client);
            for (String badKey : new String[]{"not-a-uuid", "12345", "uuid-with-invalid-chars!"}) {
                try {
                    repository.upload(new byte[]{1,2,3}, "", java.util.List.of(), badKey, null);
                    fail();
                } catch (ApiFailure failure) {
                    assertEquals(ApiFailure.Kind.VALIDATION, failure.kind);
                }
            }
            assertEquals(0, count.get());
        }
    }
    @Test public void idempotencyConflictReturns409Failure() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(409, "{\"error\":\"IDEMPOTENCY_CONFLICT\"}"))) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            try {
                new MomentRepository(client).upload(new byte[]{1,2,3}, "Conflict", java.util.List.of(), key, null);
                fail();
            } catch (ApiFailure failure) {
                assertEquals(409, failure.status);
            }
        }
    }

    @Test public void receivedMomentsPropagatesQueryAndParsesItems() throws Exception {
        String momentId = UUID.randomUUID().toString();
        String senderId = UUID.randomUUID().toString();
        String json = "{\"items\":[{\"id\":\"" + momentId + "\",\"sender\":{\"id\":\"" + senderId
                + "\",\"username\":\"alice\",\"displayName\":\"Alice\"},\"caption\":\"Hello\",\"createdAt\":\"2026-10-18T00:30:00Z\"}],"
                + "\"page\":0,\"hasNext\":false}";
        try (var server = new TestHttpServer(request -> {
            assertNotNull(request.authorization);
            assertEquals("/api/moments?type=received&page=0", request.path);
            return new TestHttpServer.Reply(200, json);
        })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            var page = new MomentRepository(client).received(0);
            assertNotNull(page);
            assertEquals(0, page.page);
            assertFalse(page.hasNext);
            assertEquals(1, page.items.size());
            var item = page.items.get(0);
            assertEquals(momentId, item.id);
            assertEquals("alice", item.sender.username);
            assertEquals("Alice", item.sender.displayName);
            assertEquals("Hello", item.caption);
            assertEquals("2026-10-18T00:30:00Z", item.createdAt);
            assertTrue(item.valid());
        }
    }

    @Test public void receivedMomentsValidatesPageNumber() throws Exception {
        var count = new AtomicInteger();
        try (var server = new TestHttpServer(request -> { count.incrementAndGet(); return new TestHttpServer.Reply(500, "{}"); })) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            var repo = new MomentRepository(client);
            try {
                repo.received(-1);
                fail();
            } catch (ApiFailure failure) {
                assertEquals(ApiFailure.Kind.VALIDATION, failure.kind);
            }
            assertEquals(0, count.get());
        }
    }

    @Test public void receivedMomentsRejectsInvalidItemData() throws Exception {
        String invalidJson = "{\"items\":[{\"id\":\"not-uuid\",\"sender\":{\"id\":\"" + UUID.randomUUID().toString()
                + "\",\"username\":\"alice\",\"displayName\":\"Alice\"},\"caption\":\"Hi\",\"createdAt\":\"2026-10-18T00:00:00Z\"}],"
                + "\"page\":0,\"hasNext\":false}";
        try (var server = new TestHttpServer(request -> new TestHttpServer.Reply(200, invalidJson))) {
            var client = new ApiClient(server.baseUrl(), new Memory()); login(client);
            try {
                new MomentRepository(client).received(0);
                fail();
            } catch (ApiFailure failure) {
                assertEquals(ApiFailure.Kind.SERVER, failure.kind);
            }
        }
    }
}
