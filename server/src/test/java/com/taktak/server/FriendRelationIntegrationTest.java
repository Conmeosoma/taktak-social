package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.friend.FriendService;
import com.taktak.server.user.UserRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.*;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FriendRelationIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String base;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "friend-relation-tests"));
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

    @AfterAll void stop() throws Exception {
        try { if (context != null) { context.close(); } }
        finally { if (postgres != null) { postgres.close(); } }
    }

    @Test void acceptCreatesCanonicalFriendshipVisibleOnlyToBothParticipants() throws Exception {
        var first = account(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"));
        var second = account(UUID.fromString("80000000-0000-0000-0000-000000000000"));
        var third = account(); UUID request = invite(first, second);
        var response = decide(second, request, "accept");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(json.readTree(response.body()).get("status").asText()).isEqualTo("accepted");
        assertThat(friendCount(first, second)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT user_low_id FROM friendships WHERE user_high_id=?", UUID.class, second.id)).isEqualTo(first.id);
        assertThat(jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, request)).isNotNull();
        assertThat(items(first).get(0).get("id").asText()).isEqualTo(second.id.toString());
        assertThat(items(second).get(0).get("id").asText()).isEqualTo(first.id.toString());
        assertThat(items(first).get(0).size()).isEqualTo(3);
        assertThat(items(third).isEmpty()).isTrue();
        assertThat(call("GET", "/api/friends", first.token, null).body()).doesNotContain("email", "password", "avatar", "token");
        assertThat(pending(second).isEmpty()).isTrue();
        assertThat(send(first, second).statusCode()).isEqualTo(409);
    }

    @Test void reverseSenderDirectionStillNormalizesUuidPair() throws Exception {
        var first = account(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var second = account(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        UUID request = invite(second, first);
        assertThat(decide(first, request, "accept").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT user_low_id FROM friendships WHERE user_high_id=?", UUID.class, second.id)).isEqualTo(first.id);
        assertThat(friendCount(first, second)).isEqualTo(1);
    }

    @Test void rejectIsIdempotentAndDoesNotCreateFriendship() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        assertThat(decide(second, request, "reject").statusCode()).isEqualTo(200);
        Object responded = jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, request);
        assertThat(responded).isNotNull();
        assertThat(decide(second, request, "reject").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, request)).isEqualTo(responded);
        assertThat(decide(second, request, "accept").statusCode()).isEqualTo(409);
        assertThat(friendCount(first, second)).isZero(); assertThat(pending(second).isEmpty()).isTrue();
        assertThat(send(first, second).statusCode()).isEqualTo(201);
    }

    @Test void onlyReceiverCanRespondAndUnknownIdsAreHidden() throws Exception {
        var first = account(); var second = account(); var third = account(); UUID request = invite(first, second);
        for (var actor : List.of(first, third)) {
            for (String action : List.of("accept", "reject")) {
                var response = decide(actor, request, action); assertThat(response.statusCode()).isEqualTo(404);
                assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("FRIEND_REQUEST_NOT_FOUND");
            }
        }
        assertThat(decide(second, UUID.randomUUID(), "accept").statusCode()).isEqualTo(404);
        assertThat(status(request)).isEqualTo("pending"); assertThat(friendCount(first, second)).isZero();
    }

    @Test void invalidActionsAndBodiesAreRejectedWithoutChangingRequest() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        for (String body : List.of("{}", "null", "[]", "{", "{\"action\":null}", "{\"action\":\"ACCEPT\"}",
                "{\"action\":\" accept\"}", "{\"action\":\"accepted\"}", "{\"action\":\"\"}")) {
            assertThat(call("PUT", "/api/friends/requests/" + request, second.token, body).statusCode()).isEqualTo(400);
        }
        assertThat(call("PUT", "/api/friends/requests/bad", second.token, "{\"action\":\"accept\"}").statusCode()).isEqualTo(400);
        var unsupported = HttpRequest.newBuilder(URI.create(base + "/api/friends/requests/" + request))
                .header("Authorization", "Bearer " + second.token).header("Content-Type", "text/plain")
                .PUT(HttpRequest.BodyPublishers.ofString("accept")).build();
        assertThat(client.send(unsupported, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(415);
        assertThat(status(request)).isEqualTo("pending");
        var response = call("PUT", "/api/friends/requests/" + request, second.token,
                json.writeValueAsString(Map.of("action", "accept", "senderId", second.id, "receiverId", first.id, "status", "rejected")));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("receiverId").asText()).isEqualTo(second.id.toString());
        assertThat(status(request)).isEqualTo("accepted");
    }

    @Test void concurrentAcceptRetriesCreateOnlyOneFriendship() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        var codes = race(12, index -> decide(second, request, "accept").statusCode());
        assertThat(codes).containsOnly(200); assertThat(friendCount(first, second)).isEqualTo(1);
        assertThat(status(request)).isEqualTo("accepted");
    }

    @Test void concurrentAcceptAndRejectHaveOneWinningDecision() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        var codes = race(12, index -> decide(second, request, index % 2 == 0 ? "accept" : "reject").statusCode());
        assertThat(codes).filteredOn(code -> code == 200).hasSize(6);
        assertThat(codes).filteredOn(code -> code == 409).hasSize(6);
        assertThat(friendCount(first, second)).isEqualTo("accepted".equals(status(request)) ? 1 : 0);
    }

    @Test void eitherParticipantCanUnfriendAndOldAcceptCannotResurrect() throws Exception {
        for (boolean reverse : List.of(false, true)) {
            var first = account(); var second = account(); UUID request = invite(first, second);
            assertThat(decide(second, request, "accept").statusCode()).isEqualTo(200);
            var actor = reverse ? second : first; var target = reverse ? first : second;
            var response = unfriend(actor, target); assertThat(response.statusCode()).isEqualTo(204);
            assertThat(response.body()).isEmpty(); assertThat(unfriend(actor, target).statusCode()).isEqualTo(204);
            assertThat(friendCount(first, second)).isZero();
            assertThat(decide(second, request, "accept").statusCode()).isEqualTo(200);
            assertThat(friendCount(first, second)).isZero();
            UUID fresh = invite(first, second);
            assertThat(decide(second, fresh, "accept").statusCode()).isEqualTo(200);
            assertThat(friendCount(first, second)).isEqualTo(1);
        }
    }

    @Test void thirdPartyCannotDeleteAnUnrelatedFriendship() throws Exception {
        var first = account(); var second = account(); var third = account();
        assertThat(decide(second, invite(first, second), "accept").statusCode()).isEqualTo(200);
        assertThat(unfriend(third, second).statusCode()).isEqualTo(204);
        UUID friendshipId = jdbc.queryForObject("SELECT id FROM friendships WHERE user_low_id=least(?,?) AND user_high_id=greatest(?,?)",
                UUID.class, first.id, second.id, first.id, second.id);
        assertThat(call("DELETE", "/api/friends/" + friendshipId, third.token, null).statusCode()).isEqualTo(404);
        assertThat(friendCount(first, second)).isEqualTo(1);
    }

    @Test void blockRemovesFriendshipAndHidesBothDirections() throws Exception {
        var first = account(); var second = account();
        assertThat(decide(second, invite(first, second), "accept").statusCode()).isEqualTo(200);
        assertThat(block(first, second).statusCode()).isEqualTo(204);
        Object created = jdbc.queryForObject("SELECT created_at FROM blocks WHERE blocker_id=? AND blocked_id=?", Object.class, first.id, second.id);
        assertThat(block(first, second).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT created_at FROM blocks WHERE blocker_id=? AND blocked_id=?", Object.class, first.id, second.id)).isEqualTo(created);
        assertThat(friendCount(first, second)).isZero(); assertThat(blockCount(first, second)).isEqualTo(1);
        assertThat(items(first).isEmpty()).isTrue(); assertThat(items(second).isEmpty()).isTrue();
        assertThat(send(first, second).statusCode()).isEqualTo(404); assertThat(send(second, first).statusCode()).isEqualTo(404);
        for (var pair : List.of(List.of(first, second), List.of(second, first))) {
            var result = call("GET", "/api/users/search?q=" + pair.get(1).username, pair.get(0).token, null);
            assertThat(result.statusCode()).isEqualTo(200); assertThat(json.readTree(result.body()).isEmpty()).isTrue();
        }
    }

    @Test void blockRejectsPendingInEitherDirectionAndUnblockDoesNotRestore() throws Exception {
        for (boolean reverse : List.of(false, true)) {
            var first = account(); var second = account();
            UUID request = reverse ? invite(second, first) : invite(first, second);
            var receiver = reverse ? first : second;
            assertThat(block(first, second).statusCode()).isEqualTo(204);
            assertThat(status(request)).isEqualTo("rejected"); assertThat(pending(receiver).isEmpty()).isTrue();
            assertThat(jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, request)).isNotNull();
            assertThat(decide(receiver, request, "accept").statusCode()).isEqualTo(404);
            assertThat(unblock(first, second).statusCode()).isEqualTo(204);
            assertThat(unblock(first, second).statusCode()).isEqualTo(204);
            assertThat(friendCount(first, second)).isZero(); assertThat(pending(receiver).isEmpty()).isTrue();
            assertThat(decide(receiver, request, "accept").statusCode()).isEqualTo(409);
            assertThat(send(first, second).statusCode()).isEqualTo(201);
        }
    }

    @Test void unblockOnlyRemovesOwnDirection() throws Exception {
        var first = account(); var second = account();
        assertThat(block(first, second).statusCode()).isEqualTo(204);
        assertThat(block(second, first).statusCode()).isEqualTo(204);
        assertThat(unblock(first, second).statusCode()).isEqualTo(204);
        assertThat(blockCount(first, second)).isZero(); assertThat(blockCount(second, first)).isEqualTo(1);
        assertThat(send(first, second).statusCode()).isEqualTo(404);
        assertThat(unblock(first, second).statusCode()).isEqualTo(204);
        assertThat(blockCount(second, first)).isEqualTo(1);
        assertThat(unblock(second, first).statusCode()).isEqualTo(204);
        assertThat(send(first, second).statusCode()).isEqualTo(201);
    }

    @Test void blockPreservesProcessedHistoryAndUnrelatedRelations() throws Exception {
        var first = account(); var second = account(); var third = account(); var fourth = account();
        UUID history = invite(first, second); assertThat(decide(second, history, "accept").statusCode()).isEqualTo(200);
        assertThat(unfriend(first, second).statusCode()).isEqualTo(204);
        UUID pending = invite(second, first);
        assertThat(decide(third, invite(first, third), "accept").statusCode()).isEqualTo(200);
        UUID unrelated = invite(fourth, first);
        assertThat(block(first, second).statusCode()).isEqualTo(204);
        assertThat(status(history)).isEqualTo("accepted"); assertThat(status(pending)).isEqualTo("rejected");
        assertThat(status(unrelated)).isEqualTo("pending"); assertThat(friendCount(first, third)).isEqualTo(1);
    }

    @Test void selfUnknownAndMalformedRelationTargetsAreSafe() throws Exception {
        var actor = account();
        for (String method : List.of("POST", "DELETE")) {
            assertThat(call(method, "/api/blocks/" + actor.id, actor.token, null).statusCode()).isEqualTo(400);
            assertThat(call(method, "/api/blocks/" + UUID.randomUUID(), actor.token, null).statusCode()).isEqualTo(404);
            assertThat(call(method, "/api/blocks/bad", actor.token, null).statusCode()).isEqualTo(400);
        }
        assertThat(unfriend(actor, actor).statusCode()).isEqualTo(400);
        assertThat(call("DELETE", "/api/friends/" + UUID.randomUUID(), actor.token, null).statusCode()).isEqualTo(404);
        assertThat(call("DELETE", "/api/friends/bad", actor.token, null).statusCode()).isEqualTo(400);
    }

    @Test void friendsArePaginatedAndExcludeBlockedOrNonFriends() throws Exception {
        var actor = account(); String prefix = "list_" + UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 21; i++) {
            var friend = account(UUID.randomUUID(), prefix + String.format(Locale.ROOT, "%02d", i));
            seedFriendship(actor, friend);
        }
        var hidden = account(); seedFriendship(actor, hidden);
        jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", actor.id, hidden.id);
        var reverse = account(); seedFriendship(actor, reverse);
        jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", reverse.id, actor.id);
        invite(account(), actor);
        var response = call("GET", "/api/friends?page=0", actor.token, null);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        var first = json.readTree(response.body()); var second = json.readTree(call("GET", "/api/friends?page=1", actor.token, null).body());
        assertThat(first.get("items").size()).isEqualTo(20); assertThat(first.get("hasNext").asBoolean()).isTrue();
        assertThat(second.get("items").size()).isEqualTo(1); assertThat(second.get("hasNext").asBoolean()).isFalse();
        assertThat(first.get("items").get(0).get("username").asText()).isEqualTo(prefix + "00");
        assertThat(second.get("items").get(0).get("username").asText()).isEqualTo(prefix + "20");
        for (String page : List.of("-1", "10001", "bad", "2147483648")) {
            assertThat(call("GET", "/api/friends?page=" + page, actor.token, null).statusCode()).isEqualTo(400);
        }
        assertThat(json.readTree(call("GET", "/api/friends?page=2", actor.token, null).body()).get("items").isEmpty()).isTrue();
    }

    @Test void allNewEndpointsRequireValidJwtAndExistingSubject() throws Exception {
        var actor = account(); UUID request = invite(account(), actor);
        for (String token : Arrays.asList(null, "invalid.jwt.token", signedToken(actor.id, Instant.now().minusSeconds(60)),
                signedToken(UUID.randomUUID(), Instant.now().plusSeconds(60)))) {
            assertThat(call("PUT", "/api/friends/requests/" + request, token, "{\"action\":\"accept\"}").statusCode()).isEqualTo(401);
            assertThat(call("GET", "/api/friends", token, null).statusCode()).isEqualTo(401);
            assertThat(call("DELETE", "/api/friends/" + actor.id, token, null).statusCode()).isEqualTo(401);
            assertThat(call("POST", "/api/blocks/" + actor.id, token, null).statusCode()).isEqualTo(401);
            assertThat(call("DELETE", "/api/blocks/" + actor.id, token, null).statusCode()).isEqualTo(401);
        }
        assertThat(status(request)).isEqualTo("pending");
    }

    @Test void failedFriendshipInsertRollsBackAcceptedRequest() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        jdbc.execute("CREATE FUNCTION qa_reject_friendship() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'qa rollback'; END; $$");
        jdbc.execute("CREATE TRIGGER qa_reject_friendship BEFORE INSERT ON friendships FOR EACH ROW EXECUTE FUNCTION qa_reject_friendship()");
        try { assertThat(decide(second, request, "accept").statusCode()).isEqualTo(500); }
        finally { jdbc.execute("DROP TRIGGER qa_reject_friendship ON friendships"); jdbc.execute("DROP FUNCTION qa_reject_friendship()"); }
        assertThat(status(request)).isEqualTo("pending"); assertThat(friendCount(first, second)).isZero();
        assertThat(jdbc.queryForObject("SELECT responded_at FROM friend_requests WHERE id=?", Object.class, request)).isNull();
        assertThat(decide(second, request, "accept").statusCode()).isEqualTo(200);
    }

    @Test void failedPendingRejectionRollsBackBlockAndFriendshipRemoval() throws Exception {
        var first = account(); var second = account(); seedFriendship(first, second);
        UUID request = inviteSeed(first, second);
        jdbc.execute("CREATE FUNCTION qa_reject_block_cleanup() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'qa rollback'; END; $$");
        jdbc.execute("CREATE TRIGGER qa_reject_block_cleanup BEFORE UPDATE ON friend_requests FOR EACH ROW EXECUTE FUNCTION qa_reject_block_cleanup()");
        try { assertThat(block(first, second).statusCode()).isEqualTo(500); }
        finally { jdbc.execute("DROP TRIGGER qa_reject_block_cleanup ON friend_requests"); jdbc.execute("DROP FUNCTION qa_reject_block_cleanup()"); }
        assertThat(blockCount(first, second)).isZero(); assertThat(friendCount(first, second)).isEqualTo(1);
        assertThat(status(request)).isEqualTo("pending");
        assertThat(block(first, second).statusCode()).isEqualTo(204);
        assertThat(blockCount(first, second)).isEqualTo(1); assertThat(friendCount(first, second)).isZero();
        assertThat(status(request)).isEqualTo("rejected");
    }

    @Test void concurrentAcceptAndBlockCannotLeaveFriendshipOrPending() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            var first = account(); var second = account(); UUID request = invite(first, second);
            var codes = race(12, index -> {
                if (index % 2 == 0) { return decide(second, request, "accept").statusCode(); }
                return block(index % 3 == 0 ? first : second, index % 3 == 0 ? second : first).statusCode();
            });
            assertThat(codes).allMatch(code -> Set.of(200, 204, 404).contains(code));
            assertThat(codes).filteredOn(code -> code == 204).hasSize(6);
            assertThat(friendCount(first, second)).isZero(); assertThat(pendingCount(first, second)).isZero();
            assertThat(blockCount(first, second) + blockCount(second, first)).isEqualTo(2);
        }
    }

    @Test void concurrentSendAndBlockCannotLeavePending() throws Exception {
        var first = account(); var second = account();
        var codes = race(12, index -> index % 2 == 0 ? send(first, second).statusCode() : block(second, first).statusCode());
        assertThat(codes).allMatch(code -> Set.of(201, 204, 404, 409).contains(code));
        assertThat(codes).filteredOn(code -> code == 204).hasSize(6);
        assertThat(pendingCount(first, second)).isZero(); assertThat(friendCount(first, second)).isZero();
        assertThat(blockCount(second, first)).isEqualTo(1);
    }

    @Test void waitingAcceptReadsRejectionCommittedBeforeItGetsPairLock() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        var pool = Executors.newSingleThreadExecutor();
        var future = new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
        try {
            transaction().execute(status -> {
                context.getBean(UserRepository.class).lockPair(first.id, second.id);
                future.set(pool.submit(() -> decide(second, request, "accept").statusCode()));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    // pg_stat_activity cache snapshot theo transaction; lấy trạng thái mới mỗi lượt.
                    jdbc.execute("select pg_stat_clear_snapshot()");
                    waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() "
                            + "AND wait_event_type='Lock' AND query ILIKE '%users%'", Integer.class) > 0;
                    if (waiting) { break; }
                    try { Thread.sleep(20); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
                }
                assertThat(waiting).as("HTTP acceptance reached the pair lock").isTrue();
                context.getBean(FriendService.class).respond(second.id, request, "reject");
                return null;
            });
            assertThat(future.get().get(10, TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(status(request)).isEqualTo("rejected"); assertThat(friendCount(first, second)).isZero();
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentUnfriendAndAcceptRetriesNeverResurrectAfterFinalUnfriend() throws Exception {
        var first = account(); var second = account(); UUID request = invite(first, second);
        var codes = race(12, index -> index % 2 == 0 ? decide(second, request, "accept").statusCode()
                : unfriend(first, second).statusCode());
        assertThat(codes).filteredOn(code -> code == 200).hasSize(6);
        assertThat(codes).filteredOn(code -> code == 204).hasSize(6);
        assertThat(status(request)).isEqualTo("accepted");
        assertThat(friendCount(first, second)).isBetween(0, 1);
        assertThat(unfriend(second, first).statusCode()).isEqualTo(204);
        assertThat(decide(second, request, "accept").statusCode()).isEqualTo(200);
        assertThat(friendCount(first, second)).isZero();
    }

    private record Account(UUID id, String username, String token) { }
    private Account account() { return account(UUID.randomUUID()); }
    private Account account(UUID id) { return account(id, "relation_" + id.toString().replace("-", "")); }
    private Account account(UUID id, String username) {
        jdbc.update("INSERT INTO users(id,email,username,password_hash,display_name) VALUES(?,?,?,?,?)",
                id, id + "@example.test", username, "fixture-not-login", "Relation QA");
        return new Account(id, username, transaction().execute(status -> context.getBean(TokenService.class).issue(id).accessToken()));
    }
    private TransactionTemplate transaction() { return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)); }
    private String signedToken(UUID id, Instant expires) {
        return context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("https://taktak.invalid").audience(List.of("taktak-app")).subject(id.toString())
                        .issuedAt(Instant.now().minusSeconds(120)).expiresAt(expires).build())).getTokenValue();
    }
    private UUID invite(Account sender, Account receiver) throws Exception {
        var response = send(sender, receiver); assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(json.readTree(response.body()).get("id").asText());
    }
    private UUID inviteSeed(Account sender, Account receiver) {
        UUID id = UUID.randomUUID(); jdbc.update("INSERT INTO friend_requests(id,sender_id,receiver_id) VALUES(?,?,?)", id, sender.id, receiver.id); return id;
    }
    private void seedFriendship(Account first, Account second) {
        jdbc.update("INSERT INTO friendships(user_low_id,user_high_id) VALUES(least(?,?),greatest(?,?))", first.id, second.id, first.id, second.id);
    }
    private HttpResponse<String> send(Account sender, Account receiver) throws Exception {
        return call("POST", "/api/friends/requests", sender.token, json.writeValueAsString(Map.of("receiverId", receiver.id)));
    }
    private HttpResponse<String> decide(Account actor, UUID request, String action) throws Exception {
        return call("PUT", "/api/friends/requests/" + request, actor.token, json.writeValueAsString(Map.of("action", action)));
    }
    private HttpResponse<String> block(Account actor, Account target) throws Exception { return call("POST", "/api/blocks/" + target.id, actor.token, null); }
    private HttpResponse<String> unblock(Account actor, Account target) throws Exception { return call("DELETE", "/api/blocks/" + target.id, actor.token, null); }
    private HttpResponse<String> unfriend(Account actor, Account target) throws Exception { return call("DELETE", "/api/friends/" + target.id, actor.token, null); }
    private JsonNode items(Account actor) throws Exception { return json.readTree(call("GET", "/api/friends", actor.token, null).body()).get("items"); }
    private JsonNode pending(Account actor) throws Exception { return json.readTree(call("GET", "/api/friends/requests", actor.token, null).body()).get("items"); }
    private String status(UUID id) { return jdbc.queryForObject("SELECT status FROM friend_requests WHERE id=?", String.class, id); }
    private int friendCount(Account first, Account second) { return jdbc.queryForObject("SELECT count(*) FROM friendships WHERE user_low_id=least(?,?) AND user_high_id=greatest(?,?)", Integer.class, first.id, second.id, first.id, second.id); }
    private int blockCount(Account actor, Account target) { return jdbc.queryForObject("SELECT count(*) FROM blocks WHERE blocker_id=? AND blocked_id=?", Integer.class, actor.id, target.id); }
    private int pendingCount(Account first, Account second) { return jdbc.queryForObject("SELECT count(*) FROM friend_requests WHERE status='pending' AND ((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?))", Integer.class, first.id, second.id, second.id, first.id); }
    private List<Integer> race(int count, CheckedCall action) throws Exception {
        var pool = Executors.newFixedThreadPool(count); var start = new CountDownLatch(1); var futures = new ArrayList<Future<Integer>>();
        try {
            for (int i = 0; i < count; i++) {
                int index = i; futures.add(pool.submit(() -> { if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("QA timeout"); } return action.run(index); }));
            }
            start.countDown(); var codes = new ArrayList<Integer>();
            for (var future : futures) { codes.add(future.get(20, TimeUnit.SECONDS)); }
            return codes;
        } finally { start.countDown(); pool.shutdownNow(); }
    }
    @FunctionalInterface private interface CheckedCall { int run(int index) throws Exception; }
    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        if (body != null) { request.header("Content-Type", "application/json"); }
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
