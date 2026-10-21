package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.device.DeviceService;
import com.taktak.server.device.DeviceTokenRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MomentPushIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private Path storage;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private RecordingPushSender pushSender;
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class PushFixture {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        RecordingPushSender recordingPushSender() { return new RecordingPushSender(); }
    }
    private DeviceService deviceService;
    private DeviceTokenRepository deviceTokenRepository;

    @BeforeAll
    void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "moment-push-tests"));
        storage = Files.createTempDirectory(root, "private-images-");
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        context = (ServletWebServerApplicationContext) new SpringApplication(TaktakApplication.class, PushFixture.class).run(
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key),
                "--taktak.storage.root=" + storage, "--server.port=0", "--server.address=127.0.0.1",
                "--debug=false", "--logging.level.root=WARN", "--taktak.outbox.enabled=false");
        jdbc = context.getBean(JdbcTemplate.class);
        pushSender = context.getBean(RecordingPushSender.class);
        deviceService = context.getBean(DeviceService.class);
        deviceTokenRepository = context.getBean(DeviceTokenRepository.class);
        base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll
    void stop() throws Exception {
        try {
            if (context != null) { context.close(); }
        } finally {
            if (postgres != null) { postgres.close(); }
        }
    }

    @BeforeEach
    void resetPushSender() {
        pushSender.result=com.taktak.server.device.PushNotificationSender.Delivery.ACCEPTED;
        context.getBean(com.taktak.server.outbox.PushOutboxDispatcher.class).dispatchBatch();
        if (pushSender != null) {
            recorded(); pushSender.clearRecordedMessages();
        }
    }

    private record Account(UUID id, String username, String token) {}

    private java.util.List<RecordingPushSender.SentMessage> recorded() {
        context.getBean(com.taktak.server.outbox.PushOutboxDispatcher.class).dispatchBatch();
        return pushSender.getRecordedMessages();
    }

    private Account account() {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().replace("-", "");
        String username = "push_" + suffix;
        jdbc.update("INSERT INTO users(id,email,username,password_hash,display_name) VALUES(?,?,?,?,?)",
                id, suffix + "@example.test", username, "fixture-not-login", "Push QA");
        String token = new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status ->
                context.getBean(TokenService.class).issue(id).accessToken());
        return new Account(id, username, token);
    }

    private void befriend(Account first, Account second) {
        jdbc.update("INSERT INTO friendships(user_low_id,user_high_id) VALUES(least(?,?),greatest(?,?))",
                first.id, second.id, first.id, second.id);
    }

    private byte[] jpeg() throws Exception {
        var image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        image.flush();
        return output.toByteArray();
    }

    private HttpResponse<String> uploadWithKey(String token, String idempotencyKey, byte[] image, String caption, String... recipients) throws Exception {
        String boundary = "Taktak" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n" + caption).getBytes(StandardCharsets.UTF_8));
        for (String recipient : recipients) {
            body.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"recipientIds\"\r\n\r\n" + recipient).getBytes(StandardCharsets.UTF_8));
        }
        body.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments"))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        if (idempotencyKey != null) { request.header("Idempotency-Key", idempotencyKey); }
        return client.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deleteMoment(String token, UUID momentId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments/" + momentId))
                .timeout(java.time.Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> getImage(String token, UUID momentId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments/" + momentId + "/image"))
                .timeout(java.time.Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<String> listReceived(String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments?type=received&page=0"))
                .timeout(java.time.Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void momentCreationWithRecipientsDispatchesFcmDataMessageToAllRecipientDevices() throws Exception {
        var sender = account();
        var recipient = account();
        befriend(sender, recipient);

        // Register 2 devices for recipient
        String tokenPhone = "token_phone_" + UUID.randomUUID();
        String tokenTablet = "token_tablet_" + UUID.randomUUID();
        deviceService.registerDevice(recipient.id, "phone_device", tokenPhone);
        deviceService.registerDevice(recipient.id, "tablet_device", tokenTablet);
        recorded(); pushSender.clearRecordedMessages();

        var res = uploadWithKey(sender.token, null, jpeg(), "Hey friend", recipient.id.toString());
        assertThat(res.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(res.body()).get("id").asText());

        var recorded = recorded();
        assertThat(recorded).hasSize(2);

        var tokens = recorded.stream().map(RecordingPushSender.SentMessage::fcmToken).toList();
        assertThat(tokens).containsExactlyInAnyOrder(tokenPhone, tokenTablet);

        for (var msg : recorded) {
            assertThat(msg.data()).containsEntry("type", "moment_created");
            assertThat(msg.data()).containsEntry("momentId", momentId.toString());
            // Security: Never send image URL, caption, or bearer token in push
            assertThat(msg.data()).doesNotContainKey("imageUrl");
            assertThat(msg.data()).doesNotContainKey("caption");
            assertThat(msg.data()).doesNotContainKey("token");
        }
    }

    @Test
    void ownerOnlyMomentDoesNotDispatchPushMessage() throws Exception {
        var sender = account();
        String token = "sender_token_" + UUID.randomUUID();
        deviceService.registerDevice(sender.id, "my_device", token);
        recorded(); pushSender.clearRecordedMessages();

        var res = uploadWithKey(sender.token, null, jpeg(), "Private only me", "[]");
        assertThat(res.statusCode()).isEqualTo(201);

        assertThat(recorded()).isEmpty();
    }

    @Test
    void idempotentUploadReplayDoesNotDispatchDuplicatePushMessage() throws Exception {
        var sender = account();
        var recipient = account();
        befriend(sender, recipient);

        String token = "recipient_fcm_" + UUID.randomUUID();
        deviceService.registerDevice(recipient.id, "dev1", token);

        String idempotencyKey = UUID.randomUUID().toString();
        byte[] image = jpeg();

        // 1st upload
        recorded(); pushSender.clearRecordedMessages();
        var first = uploadWithKey(sender.token, idempotencyKey, image, "Identical", recipient.id.toString());
        assertThat(first.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(first.body()).get("id").asText());
        assertThat(recorded()).hasSize(1);

        // 2nd upload (replay)
        recorded(); pushSender.clearRecordedMessages();
        var replay = uploadWithKey(sender.token, idempotencyKey, image, "Identical", recipient.id.toString());
        assertThat(replay.statusCode()).isEqualTo(201);
        UUID replayId = UUID.fromString(json.readTree(replay.body()).get("id").asText());
        assertThat(replayId).isEqualTo(momentId);

        // Replay must NOT dispatch push again
        assertThat(recorded()).isEmpty();
    }

    @Test
    void momentDeletionBySenderDispatchesDeletedPushAndRemovesFileAndRevokesAccess() throws Exception {
        var sender = account();
        var recipient = account();
        befriend(sender, recipient);

        String token = "del_test_token_" + UUID.randomUUID();
        deviceService.registerDevice(recipient.id, "dev_del", token);

        var uploadRes = uploadWithKey(sender.token, null, jpeg(), "To delete", recipient.id.toString());
        assertThat(uploadRes.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(uploadRes.body()).get("id").asText());

        Path filePath = storage.resolve(momentId + ".jpg");
        assertThat(Files.exists(filePath)).isTrue();

        // Verify recipient can read image and see it in list
        assertThat(getImage(recipient.token, momentId).statusCode()).isEqualTo(200);
        assertThat(listReceived(recipient.token).body()).contains(momentId.toString());

        // Sender deletes moment
        recorded(); pushSender.clearRecordedMessages();
        var deleteRes = deleteMoment(sender.token, momentId);
        assertThat(deleteRes.statusCode()).isEqualTo(204);

        // Push notification moment_deleted sent to recipient
        var recorded = recorded();
        assertThat(recorded).hasSize(1);
        assertThat(recorded.get(0).fcmToken()).isEqualTo(token);
        assertThat(recorded.get(0).data()).containsEntry("type", "moment_deleted");
        assertThat(recorded.get(0).data()).containsEntry("momentId", momentId.toString());

        // File is discarded from private storage
        assertThat(Files.exists(filePath)).isFalse();

        // GET image returns 404
        assertThat(getImage(recipient.token, momentId).statusCode()).isEqualTo(404);
        assertThat(getImage(sender.token, momentId).statusCode()).isEqualTo(404);

        // Moment disappears from received list
        assertThat(listReceived(recipient.token).body()).doesNotContain(momentId.toString());
    }

    @Test
    void momentDeletionByNonSenderIsForbiddenAndDoesNotSendPushOrDeleteFile() throws Exception {
        var sender = account();
        var recipient = account();
        befriend(sender, recipient);

        String token = "non_sender_token_" + UUID.randomUUID();
        deviceService.registerDevice(recipient.id, "dev_non_sender", token);

        var uploadRes = uploadWithKey(sender.token, null, jpeg(), "Keep safe", recipient.id.toString());
        assertThat(uploadRes.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(uploadRes.body()).get("id").asText());

        Path filePath = storage.resolve(momentId + ".jpg");
        assertThat(Files.exists(filePath)).isTrue();

        recorded(); pushSender.clearRecordedMessages();
        // Recipient attempts to delete sender's moment
        var deleteRes = deleteMoment(recipient.token, momentId);
        assertThat(deleteRes.statusCode()).isEqualTo(403);

        // File still exists, no push dispatched
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(recorded()).isEmpty();
    }

    @Test
    void deletingAlreadyDeletedMomentReturnsNotFound() throws Exception {
        var sender = account();
        var uploadRes = uploadWithKey(sender.token, null, jpeg(), "Double delete", "[]");
        assertThat(uploadRes.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(uploadRes.body()).get("id").asText());

        assertThat(deleteMoment(sender.token, momentId).statusCode()).isEqualTo(204);
        // Second delete returns 404
        assertThat(deleteMoment(sender.token, momentId).statusCode()).isEqualTo(404);
    }

    @Test
    void deleteByFcmTokenRemovesStaleTokenFromDatabase() {
        var user = account();
        String token = "stale_token_" + UUID.randomUUID();
        deviceService.registerDevice(user.id, "device_stale", token);

        assertThat(deviceTokenRepository.findByFcmToken(token)).isPresent();

        deviceTokenRepository.deleteByFcmToken(token);

        assertThat(deviceTokenRepository.findByFcmToken(token)).isEmpty();
    }

    private HttpResponse<String> getLatest(String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments/latest"))
                .timeout(java.time.Duration.ofSeconds(20));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void failedPushKeepsCommittedMomentAndRetriesAfterRestartWithCurrentToken() throws Exception {
        var sender=account();var recipient=account();befriend(sender,recipient);
        deviceService.registerDevice(recipient.id,"retry-phone","old-"+UUID.randomUUID());
        var response=uploadWithKey(sender.token,null,jpeg(),"Private QA",recipient.id.toString());
        UUID id=UUID.fromString(json.readTree(response.body()).get("id").asText());
        assertThat(jdbc.queryForObject("select status from push_outbox where resource_id=?",String.class,id)).isEqualTo("PENDING");
        assertThat(pushSender.getRecordedMessages()).isEmpty();
        pushSender.result=com.taktak.server.device.PushNotificationSender.Delivery.RETRY;
        recorded();
        assertThat(getImage(recipient.token,id).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select attempts from push_outbox where resource_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select available_at>created_at from push_outbox where resource_id=?",Boolean.class,id)).isTrue();
        String current="new-"+UUID.randomUUID();deviceService.registerDevice(recipient.id,"retry-phone",current);
        jdbc.update("update push_outbox set available_at=now()-interval '1 second' where resource_id=?",id);
        pushSender.result=com.taktak.server.device.PushNotificationSender.Delivery.ACCEPTED;
        var restarted=new com.taktak.server.outbox.PushOutboxDispatcher(jdbc,pushSender,
                context.getBean(com.taktak.server.friend.FriendRequestRepository.class),
                context.getBean(PlatformTransactionManager.class),java.time.Clock.systemUTC(),8);
        restarted.dispatchBatch();
        assertThat(pushSender.getRecordedMessages()).hasSize(1);
        assertThat(pushSender.getRecordedMessages().get(0).fcmToken()).isEqualTo(current);
        assertThat(jdbc.queryForObject("select status from push_outbox where resource_id=?",String.class,id)).isEqualTo("ACCEPTED");
    }

    @Test
    void outboxRollbackDoesNotLeaveAnEvent() {
        var actor=account();var recipient=account();UUID id=UUID.randomUUID();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            context.getBean(com.taktak.server.outbox.PushOutbox.class).enqueue("moment_created",id,null,actor.id,recipient.id);
            tx.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from push_outbox where resource_id=?",Integer.class,id)).isZero();
    }

    @Test
    void disabledFcmIsNeverMarkedAcceptedAndRetryBudgetIsBounded() throws Exception {
        var actor=account();var recipient=account();befriend(actor,recipient);
        deviceService.registerDevice(recipient.id,"disabled","disabled-"+UUID.randomUUID());
        UUID id=UUID.fromString(json.readTree(uploadWithKey(actor.token,null,jpeg(),"",recipient.id.toString()).body()).get("id").asText());
        pushSender.result=com.taktak.server.device.PushNotificationSender.Delivery.DISABLED;
        for(int i=0;i<8;i++) { jdbc.update("update push_outbox set available_at=now()-interval '1 second' where resource_id=?",id);recorded(); }
        assertThat(jdbc.queryForObject("select status from push_outbox where resource_id=?",String.class,id)).isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("select last_error from push_outbox where resource_id=?",String.class,id)).isEqualTo("FCM_DISABLED");
        assertThat(pushSender.getRecordedMessages()).isEmpty();
    }

    @Test
    void unregisteredTokenIsRemovedWithoutRetryingForever() throws Exception {
        var actor=account();var recipient=account();befriend(actor,recipient);
        String token="unregistered-"+UUID.randomUUID();deviceService.registerDevice(recipient.id,"unregistered",token);
        UUID id=UUID.fromString(json.readTree(uploadWithKey(actor.token,null,jpeg(),"",recipient.id.toString()).body()).get("id").asText());
        pushSender.result=com.taktak.server.device.PushNotificationSender.Delivery.UNREGISTERED;recorded();
        assertThat(deviceTokenRepository.findByFcmToken(token)).isEmpty();
        assertThat(jdbc.queryForObject("select status from push_outbox where resource_id=?",String.class,id)).isEqualTo("CANCELLED");
    }

    @Test
    void activeLeaseIsNotStolenAndExpiredLeaseRecovers() throws Exception {
        var actor=account();var recipient=account();befriend(actor,recipient);
        deviceService.registerDevice(recipient.id,"leased","lease-"+UUID.randomUUID());
        UUID id=UUID.fromString(json.readTree(uploadWithKey(actor.token,null,jpeg(),"",recipient.id.toString()).body()).get("id").asText());
        jdbc.update("update push_outbox set status='PROCESSING',lease_id=?,lease_until=now()+interval '1 hour' where resource_id=?",UUID.randomUUID(),id);
        assertThat(recorded()).isEmpty();
        jdbc.update("update push_outbox set lease_until=now()-interval '1 second' where resource_id=?",id);
        assertThat(recorded()).hasSize(1);
    }

    @Test
    void concurrentDispatchersDoNotSendTheSameClaimedEvent() throws Exception {
        var actor=account();var recipient=account();befriend(actor,recipient);
        deviceService.registerDevice(recipient.id,"concurrent","concurrent-"+UUID.randomUUID());
        uploadWithKey(actor.token,null,jpeg(),"",recipient.id.toString());
        var dispatcher=context.getBean(com.taktak.server.outbox.PushOutboxDispatcher.class);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(dispatcher::dispatchBatch);var b=pool.submit(dispatcher::dispatchBatch);
            a.get(15,java.util.concurrent.TimeUnit.SECONDS);b.get(15,java.util.concurrent.TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertThat(recorded()).hasSize(1);
    }

    @Test
    void pendingCreationIsCancelledAfterBlockBeforeDispatch() throws Exception {
        var actor=account();var recipient=account();befriend(actor,recipient);
        deviceService.registerDevice(recipient.id,"blocked","blocked-"+UUID.randomUUID());
        UUID id=UUID.fromString(json.readTree(uploadWithKey(actor.token,null,jpeg(),"",recipient.id.toString()).body()).get("id").asText());
        jdbc.update("insert into blocks(blocker_id,blocked_id) values(?,?)",recipient.id,actor.id);
        assertThat(recorded()).isEmpty();
        assertThat(jdbc.queryForObject("select status from push_outbox where resource_id=?",String.class,id)).isEqualTo("CANCELLED");
    }

    @Test
    void latestMomentReturnsNoContentWhenNoMomentsReceived() throws Exception {
        var user = account();
        var res = getLatest(user.token);
        assertThat(res.statusCode()).isEqualTo(204);
        assertThat(res.body()).isBlank();
    }

    @Test
    void latestMomentReturnsMostRecentReceivedMoment() throws Exception {
        var alice = account();
        var bob = account();
        befriend(alice, bob);

        var uploadRes = uploadWithKey(alice.token, null, jpeg(), "Photo 1", bob.id.toString());
        assertThat(uploadRes.statusCode()).isEqualTo(201);
        UUID moment1Id = UUID.fromString(json.readTree(uploadRes.body()).get("id").asText());

        var latestRes1 = getLatest(bob.token);
        assertThat(latestRes1.statusCode()).isEqualTo(200);
        var jsonNode1 = json.readTree(latestRes1.body());
        assertThat(jsonNode1.get("id").asText()).isEqualTo(moment1Id.toString());
        assertThat(jsonNode1.get("caption").asText()).isEqualTo("Photo 1");
        assertThat(jsonNode1.get("sender").get("username").asText()).isEqualTo(alice.username);

        // Upload a second moment
        var uploadRes2 = uploadWithKey(alice.token, null, jpeg(), "Photo 2", bob.id.toString());
        assertThat(uploadRes2.statusCode()).isEqualTo(201);
        UUID moment2Id = UUID.fromString(json.readTree(uploadRes2.body()).get("id").asText());

        var latestRes2 = getLatest(bob.token);
        assertThat(latestRes2.statusCode()).isEqualTo(200);
        var jsonNode2 = json.readTree(latestRes2.body());
        assertThat(jsonNode2.get("id").asText()).isEqualTo(moment2Id.toString());
        assertThat(jsonNode2.get("caption").asText()).isEqualTo("Photo 2");
    }

    @Test
    void latestMomentSkipsDeletedMoments() throws Exception {
        var alice = account();
        var bob = account();
        befriend(alice, bob);

        // Photo 1
        var uploadRes1 = uploadWithKey(alice.token, null, jpeg(), "First Photo", bob.id.toString());
        UUID moment1Id = UUID.fromString(json.readTree(uploadRes1.body()).get("id").asText());

        // Photo 2
        var uploadRes2 = uploadWithKey(alice.token, null, jpeg(), "Second Photo", bob.id.toString());
        UUID moment2Id = UUID.fromString(json.readTree(uploadRes2.body()).get("id").asText());

        // Latest is Photo 2
        assertThat(json.readTree(getLatest(bob.token).body()).get("id").asText()).isEqualTo(moment2Id.toString());

        // Alice deletes Photo 2
        assertThat(deleteMoment(alice.token, moment2Id).statusCode()).isEqualTo(204);

        // Latest falls back to Photo 1
        var latestRes = getLatest(bob.token);
        assertThat(latestRes.statusCode()).isEqualTo(200);
        assertThat(json.readTree(latestRes.body()).get("id").asText()).isEqualTo(moment1Id.toString());

        // Alice deletes Photo 1
        assertThat(deleteMoment(alice.token, moment1Id).statusCode()).isEqualTo(204);

        // Now latest returns 204 No Content
        assertThat(getLatest(bob.token).statusCode()).isEqualTo(204);
    }

    @Test
    void latestMomentRequiresAuthentication() throws Exception {
        var res = getLatest(null);
        assertThat(res.statusCode()).isEqualTo(401);
    }
}
