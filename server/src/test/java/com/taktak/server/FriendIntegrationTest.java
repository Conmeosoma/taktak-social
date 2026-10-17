package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.friend.SearchRateLimiter;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FriendIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String base;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "friend-tests"));
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key),
                "--taktak.storage.root=" + Files.createTempDirectory(root, "private-images-"),
                "--server.port=0", "--server.address=127.0.0.1", "--debug=false", "--logging.level.root=WARN");
        jdbc = context.getBean(JdbcTemplate.class);
        base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @BeforeEach void resetOnlyTestLimiter() {
        var limiter = context.getBean(SearchRateLimiter.class);
        synchronized (limiter) {
            ((Map<?, ?>) Objects.requireNonNull(ReflectionTestUtils.getField(limiter, "windows"))).clear();
        }
    }

    @AfterAll void stop() throws Exception {
        try { if (context != null) { context.close(); } }
        finally { if (postgres != null) { postgres.close(); } }
    }

    @Test void searchIsNormalizedPrefixOnlyAndReturnsPublicFields() throws Exception {
        var actor = account("s" + suffix()); var target = account("find_" + suffix());
        var response = search(actor, "  " + target.username.toUpperCase(Locale.ROOT) + "  ");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        JsonNode body = json.readTree(response.body()); assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).size()).isEqualTo(3);
        assertThat(body.get(0).get("id").asText()).isEqualTo(target.id.toString());
        assertThat(body.get(0).get("username").asText()).isEqualTo(target.username);
        assertThat(response.body()).doesNotContain("email", "password", "avatar", "createdAt", "token");
        assertThat(json.readTree(search(actor, actor.username).body()).isEmpty()).isTrue();
        assertThat(json.readTree(search(actor, target.username.substring(2)).body()).isEmpty()).isTrue();
    }

    @Test void underscoreIsLiteralAndResultIsBoundedAndOrdered() throws Exception {
        var actor = account("a" + suffix()); String prefix = "p" + suffix();
        account(prefix + "xother");
        for (int i = 24; i >= 0; i--) { account(prefix + "_" + String.format(Locale.ROOT, "%02d", i)); }
        var body = json.readTree(search(actor, prefix + "_").body());
        assertThat(body.size()).isEqualTo(20);
        assertThat(body.get(0).get("username").asText()).isEqualTo(prefix + "_00");
        assertThat(body.get(19).get("username").asText()).isEqualTo(prefix + "_19");
    }

    @Test void invalidSearchIsSafeAndDoesNotReflectQuery() throws Exception {
        var actor = account("a" + suffix());
        for (String query : List.of("", " ", "%", "a%b", "a' OR 1=1", "a/b", "tên", "a".repeat(256))) {
            var response = search(actor, query); assertThat(response.statusCode()).isEqualTo(400);
            var body = json.readTree(response.body());
            assertThat(body.get("code").asText()).isEqualTo("VALIDATION_ERROR");
            assertThat(body.get("path").asText()).isEqualTo("/api/users/search");
            assertThat(response.body()).doesNotContain("SQLException", "password_hash", "OR 1=1");
        }
        assertThat(call("GET", "/api/users/search", actor.token, null).statusCode()).isEqualTo(400);
        assertThat(search(actor, "a").statusCode()).isEqualTo(200);
        assertThat(search(actor, "a".repeat(255)).statusCode()).isEqualTo(200);
    }

    @Test void blocksInEitherDirectionHideSearchAndPreventRequests() throws Exception {
        var actor = account("a" + suffix());
        for (boolean reverse : List.of(false, true)) {
            var target = account("b" + suffix());
            jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)",
                    reverse ? target.id : actor.id, reverse ? actor.id : target.id);
            assertThat(json.readTree(search(actor, target.username).body()).isEmpty()).isTrue();
            assertThat(json.readTree(search(target, actor.username).body()).isEmpty()).isTrue();
            var blocked = send(actor, target.id); assertThat(blocked.statusCode()).isEqualTo(404);
            assertThat(send(target, actor.id).statusCode()).isEqualTo(404);
            assertThat(json.readTree(blocked.body()).get("code").asText()).isEqualTo("FRIEND_TARGET_UNAVAILABLE");
            assertThat(pairCount(actor.id, target.id)).isZero();
        }
        var absent = send(actor, UUID.randomUUID()); assertThat(absent.statusCode()).isEqualTo(404);
        assertThat(json.readTree(absent.body()).get("code").asText()).isEqualTo("FRIEND_TARGET_UNAVAILABLE");
    }

    @Test void sendUsesJwtIdentityAndOnlyReceiverCanListIncoming() throws Exception {
        var sender = account("a" + suffix()); var receiver = account("b" + suffix()); var third = account("c" + suffix());
        var response = call("POST", "/api/friends/requests", sender.token,
                json.writeValueAsString(Map.of("receiverId", receiver.id, "senderId", third.id, "status", "accepted")));
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        var body = json.readTree(response.body()); UUID id = UUID.fromString(body.get("id").asText());
        assertThat(body.get("sender").get("id").asText()).isEqualTo(sender.id.toString());
        assertThat(body.get("sender").size()).isEqualTo(3); assertThat(body.get("status").asText()).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT sender_id FROM friend_requests WHERE id=?", UUID.class, id)).isEqualTo(sender.id);
        assertThat(jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, id)).isNull();
        var incoming = call("GET", "/api/friends/requests", receiver.token, null);
        assertThat(incoming.statusCode()).isEqualTo(200);
        assertThat(json.readTree(incoming.body()).get("items").get(0).get("id").asText()).isEqualTo(id.toString());
        for (var other : List.of(sender, third)) {
            assertThat(json.readTree(call("GET", "/api/friends/requests", other.token, null).body()).get("items").isEmpty()).isTrue();
        }
        assertThat(incoming.body()).doesNotContain("email", "password", "avatarPath", "token");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM friendships WHERE user_low_id=least(?,?) AND user_high_id=greatest(?,?)",
                Integer.class, sender.id, receiver.id, sender.id, receiver.id)).isZero();
    }

    @Test void selfAndMalformedRequestsAreRejectedWithoutWrites() throws Exception {
        var actor = account("a" + suffix());
        assertThat(send(actor, actor.id).statusCode()).isEqualTo(400);
        for (String body : List.of("{}", "{\"receiverId\":null}", "{\"receiverId\":\"bad\"}", "[]", "{", "null")) {
            assertThat(call("POST", "/api/friends/requests", actor.token, body).statusCode()).isEqualTo(400);
        }
        var request = HttpRequest.newBuilder(URI.create(base + "/api/friends/requests"))
                .header("Authorization", "Bearer " + actor.token).header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("not-json")).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(415);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM friend_requests WHERE sender_id=?", Integer.class, actor.id)).isZero();
    }

    @Test void duplicateAndReversePendingRequestsReturnConflict() throws Exception {
        var first = account("a" + suffix()); var second = account("b" + suffix());
        assertThat(send(first, second.id).statusCode()).isEqualTo(201);
        for (var response : List.of(send(first, second.id), send(second, first.id))) {
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("FRIEND_REQUEST_CONFLICT");
        }
        assertThat(pairCount(first.id, second.id)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id) VALUES(?,?)", second.id, first.id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void existingFriendshipRejectsAnotherInvitation() throws Exception {
        var first = account("a" + suffix()); var second = account("b" + suffix());
        jdbc.update("INSERT INTO friendships(user_low_id,user_high_id) VALUES(least(?,?),greatest(?,?))", first.id, second.id, first.id, second.id);
        assertThat(send(first, second.id).statusCode()).isEqualTo(409);
        assertThat(send(second, first.id).statusCode()).isEqualTo(409);
        assertThat(pairCount(first.id, second.id)).isZero();
    }

    @Test void concurrentOppositeDirectionsProduceExactlyOnePendingRequest() throws Exception {
        var first = account("a" + suffix()); var second = account("b" + suffix());
        var pool = Executors.newFixedThreadPool(12); var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Integer>>();
        try {
            for (int i = 0; i < 12; i++) {
                boolean reverse = i % 2 == 0;
                futures.add(pool.submit(() -> {
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return send(reverse ? first : second, reverse ? second.id : first.id).statusCode();
                }));
            }
            start.countDown(); var statuses = new ArrayList<Integer>();
            for (var future : futures) { statuses.add(future.get(20, TimeUnit.SECONDS)); }
            assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
            assertThat(statuses).filteredOn(code -> code == 409).hasSize(11);
            assertThat(pairCount(first.id, second.id)).isEqualTo(1);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void incomingPaginationIsStableAndFiltersStatusAndBlocks() throws Exception {
        var receiver = account("a" + suffix()); var hidden = account("b" + suffix());
        for (int i = 0; i < 21; i++) {
            var sender = account("s" + suffix());
            jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id,created_at) VALUES(?,?,?)", sender.id, receiver.id,
                    java.sql.Timestamp.from(Instant.parse("2026-10-17T00:00:00Z")));
        }
        jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id) VALUES(?,?)", hidden.id, receiver.id);
        jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", receiver.id, hidden.id);
        var hiddenReverse = account("h" + suffix());
        jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id) VALUES(?,?)", hiddenReverse.id, receiver.id);
        jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", hiddenReverse.id, receiver.id);
        var rejected = account("r" + suffix());
        jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id,status) VALUES(?,?,'rejected')", rejected.id, receiver.id);
        var first = json.readTree(call("GET", "/api/friends/requests?page=0", receiver.token, null).body());
        var second = json.readTree(call("GET", "/api/friends/requests?page=1", receiver.token, null).body());
        assertThat(first.get("items").size()).isEqualTo(20); assertThat(first.get("hasNext").asBoolean()).isTrue();
        assertThat(second.get("items").size()).isEqualTo(1); assertThat(second.get("hasNext").asBoolean()).isFalse();
        var ids = new HashSet<String>(); first.get("items").forEach(item -> ids.add(item.get("id").asText()));
        assertThat(ids.add(second.get("items").get(0).get("id").asText())).isTrue();
        assertThat(call("GET", "/api/friends/requests?page=0", receiver.token, null).body()).isEqualTo(first.toString());
        for (String page : List.of("-1", "10001", "2147483648", "bad")) {
            assertThat(call("GET", "/api/friends/requests?page=" + page, receiver.token, null).statusCode()).isEqualTo(400);
        }
    }

    @Test void unauthenticatedForgedExpiredAndOrphanSubjectsAreRejected() throws Exception {
        var target = account("a" + suffix());
        Instant now = Instant.now();
        String expired = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), JwtClaimsSet.builder().issuer("https://taktak.invalid")
                        .audience(List.of("taktak-app")).subject(target.id.toString()).issuedAt(now.minusSeconds(120))
                        .expiresAt(now.minusSeconds(60)).build())).getTokenValue();
        String orphan = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), JwtClaimsSet.builder().issuer("https://taktak.invalid")
                        .audience(List.of("taktak-app")).subject(UUID.randomUUID().toString()).issuedAt(now)
                        .expiresAt(now.plusSeconds(60)).build())).getTokenValue();
        for (String token : Arrays.asList(null, "invalid.jwt.token", expired, orphan)) {
            assertThat(call("GET", "/api/users/search?q=a", token, null).statusCode()).isEqualTo(401);
            assertThat(call("GET", "/api/friends/requests", token, null).statusCode()).isEqualTo(401);
            assertThat(call("POST", "/api/friends/requests", token, json.writeValueAsString(Map.of("receiverId", target.id))).statusCode()).isEqualTo(401);
        }
    }

    @Test void searchQuotaCannotBeBypassedWithForwardedHeaders() throws Exception {
        var actor = account("a" + suffix());
        for (int i = 0; i <= 30; i++) {
            var request = HttpRequest.newBuilder(URI.create(base + "/api/users/search?q=a"))
                    .header("Authorization", "Bearer " + actor.token).header("X-Forwarded-For", "192.0.2." + i).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(i < 30 ? 200 : 429);
            if (i == 30) {
                assertThat(response.headers().firstValue("Retry-After")).hasValue("60");
                assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("SEARCH_RATE_LIMITED");
            }
        }
    }

    @Test void databaseFailureRollsBackAndAllowsRetry() throws Exception {
        var first = account("a" + suffix()); var second = account("b" + suffix());
        jdbc.execute("CREATE FUNCTION qa_reject_friend() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'qa rollback'; END; $$");
        jdbc.execute("CREATE TRIGGER qa_reject_friend BEFORE INSERT ON friend_requests FOR EACH ROW EXECUTE FUNCTION qa_reject_friend()");
        try { assertThat(send(first, second.id).statusCode()).isEqualTo(500); }
        finally { jdbc.execute("DROP TRIGGER qa_reject_friend ON friend_requests"); jdbc.execute("DROP FUNCTION qa_reject_friend()"); }
        assertThat(pairCount(first.id, second.id)).isZero();
        assertThat(send(first, second.id).statusCode()).isEqualTo(201);
    }

    @Test void rejectedHistoryDoesNotPreventNewPendingInvitation() throws Exception {
        var first = account("a" + suffix()); var second = account("b" + suffix());
        jdbc.update("INSERT INTO friend_requests(sender_id,receiver_id,status,responded_at) VALUES(?,?,'rejected',CURRENT_TIMESTAMP)", first.id, second.id);
        assertThat(send(second, first.id).statusCode()).isEqualTo(201);
        assertThat(pairCount(first.id, second.id)).isEqualTo(1);
    }

    private record Account(UUID id, String username, String token) { }
    private String suffix() { return UUID.randomUUID().toString().replace("-", ""); }
    private Account account(String username) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,username,password_hash,display_name) VALUES(?,?,?,?,?)",
                id, id + "@example.test", username, "fixture-not-login", "Friend QA");
        return new Account(id, username, token(id));
    }
    private String token(UUID id) {
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status ->
                context.getBean(TokenService.class).issue(id).accessToken());
    }
    private int pairCount(UUID first, UUID second) {
        return jdbc.queryForObject("SELECT count(*) FROM friend_requests WHERE status='pending' AND "
                + "((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?))", Integer.class, first, second, second, first);
    }
    private HttpResponse<String> search(Account actor, String query) throws Exception {
        return call("GET", "/api/users/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8), actor.token, null);
    }
    private HttpResponse<String> send(Account actor, UUID receiver) throws Exception {
        return call("POST", "/api/friends/requests", actor.token, json.writeValueAsString(Map.of("receiverId", receiver)));
    }
    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        if (body != null) { request.header("Content-Type", "application/json"); }
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
