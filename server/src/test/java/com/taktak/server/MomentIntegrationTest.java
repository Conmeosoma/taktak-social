package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.storage.PrivateImageStorage;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MomentIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private Path storage;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "moment-tests"));
        storage = Files.createTempDirectory(root, "private-images-");
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key),
                "--taktak.storage.root=" + storage, "--server.port=0", "--server.address=127.0.0.1", "--debug=false", "--logging.level.root=WARN");
        jdbc = context.getBean(JdbcTemplate.class);
        base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll void stop() throws Exception {
        try { if (context != null) { context.close(); } }
        finally { if (postgres != null) { postgres.close(); } }
    }

    @Test void ownerOnlyRoundTripHasPrivateMetadataAndStripsTrailingData() throws Exception {
        var account = account();
        byte[] jpeg = jpeg();
        var input = new ByteArrayOutputStream(); input.write(jpeg); input.write("private-exif-like-marker".getBytes(StandardCharsets.UTF_8));
        var response = upload(account.token, input.toByteArray(), "📷".repeat(50), "[]");
        assertThat(response.statusCode()).isEqualTo(201);
        var body = json.readTree(response.body());
        assertThat(body.size()).isEqualTo(1);
        UUID id = UUID.fromString(body.get("id").asText());
        assertThat(jdbc.queryForObject("SELECT sender_id FROM moments WHERE id=?", UUID.class, id)).isEqualTo(account.id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM moment_recipients WHERE moment_id=?", Integer.class, id)).isZero();
        String name = jdbc.queryForObject("SELECT file_path FROM moments WHERE id=?", String.class, id);
        assertThat(name).isEqualTo(id + ".jpg");
        assertThat(response.body()).doesNotContain("filePath", "senderId", "private-images");
        var downloaded = image(account.token, id);
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.headers().firstValue("Content-Type")).hasValue("image/jpeg");
        assertThat(downloaded.headers().firstValue("Cache-Control")).hasValue("no-store");
        assertThat(downloaded.body()).isEqualTo(Files.readAllBytes(storage.resolve(name)));
        assertThat(new String(downloaded.body(), StandardCharsets.ISO_8859_1)).doesNotContain("private-exif-like-marker");
        var decoded = ImageIO.read(new java.io.ByteArrayInputStream(downloaded.body()));
        assertThat(decoded.getWidth()).isEqualTo(80); assertThat(decoded.getHeight()).isEqualTo(80);
    }

    @Test void missingForgedAndOtherAccountCannotReadOrUpload() throws Exception {
        var owner = account(); var stranger = account();
        UUID id = uploaded(owner.token);
        assertThat(image(null, id).statusCode()).isEqualTo(401);
        assertThat(image("invalid.jwt.signature", id).statusCode()).isEqualTo(401);
        assertThat(image(stranger.token, id).statusCode()).isEqualTo(404);
        assertThat(upload(null, jpeg(), "", "[]").statusCode()).isEqualTo(401);
        assertThat(upload("invalid.jwt.signature", jpeg(), "", "[]").statusCode()).isEqualTo(401);
        assertThat(client.send(HttpRequest.newBuilder(URI.create(base + "/api/" + id + ".jpg"))
                .header("Authorization", "Bearer " + owner.token).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
    }

    @Test void rejectsRecipientsAndOverlongCaptionWithoutRowsOrFiles() throws Exception {
        var owner = account(); long files = fileCount(); int rows = rows();
        for (String recipients : List.of(UUID.randomUUID().toString(), "[\"untrusted\"]", "[],")) {
            assertThat(upload(owner.token, jpeg(), "", recipients).statusCode()).isEqualTo(400);
        }
        assertThat(upload(owner.token, jpeg(), "📷".repeat(51), "[]").statusCode()).isEqualTo(400);
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
    }

    @Test void acceptedRecipientsCanReadButOtherFriendsCannotAndTimestampsMatch() throws Exception {
        var owner = account(); var first = account(); var second = account(); var unselected = account();
        for (var friend : List.of(first, second, unselected)) { befriend(owner, friend); }
        var response = upload(owner.token, jpeg(), "For friends 📷", first.id.toString(), second.id.toString());
        assertThat(response.statusCode()).isEqualTo(201);
        UUID id = UUID.fromString(json.readTree(response.body()).get("id").asText());
        var bytes = image(owner.token, id).body();
        assertThat(image(first.token, id).statusCode()).isEqualTo(200);
        assertThat(image(first.token, id).body()).isEqualTo(bytes);
        assertThat(image(second.token, id).body()).isEqualTo(bytes);
        assertThat(image(unselected.token, id).statusCode()).isEqualTo(404);
        assertThat(image(null, id).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM moment_recipients r JOIN moments m ON m.id=r.moment_id WHERE m.id=? AND r.created_at=m.created_at", Integer.class, id)).isEqualTo(2);
        jdbc.update("UPDATE moments SET deleted_at=CURRENT_TIMESTAMP WHERE id=?", id);
        assertThat(image(first.token, id).statusCode()).isEqualTo(404);
    }

    @Test void validatesEveryRecipientWithoutPartialRowsOrFiles() throws Exception {
        var owner = account(); var friend = account(); var stranger = account(); befriend(owner, friend);
        long files = fileCount(); int rows = rows();
        for (String[] invalid : List.of(new String[]{friend.id.toString(), stranger.id.toString()},
                new String[]{friend.id.toString(), UUID.randomUUID().toString()},
                new String[]{friend.id.toString(), friend.id.toString().toUpperCase(Locale.ROOT)},
                new String[]{owner.id.toString()}, new String[]{"[]", friend.id.toString()},
                new String[]{friend.id + "," + stranger.id}, new String[]{"1-1-1-1-1"}, new String[]{""})) {
            assertThat(upload(owner.token, jpeg(), "", invalid).statusCode()).isEqualTo(400);
        }
        String[] tooMany = new String[101]; Arrays.fill(tooMany, friend.id.toString());
        assertThat(json.readTree(upload(owner.token, jpeg(), "", tooMany).body()).get("code").asText()).isEqualTo("TOO_MANY_RECIPIENTS");
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
    }

    @Test void bothBlockDirectionsRejectEvenWhenFriendshipRemainsInDatabase() throws Exception {
        var owner = account(); var friend = account(); befriend(owner, friend);
        long files = fileCount(); int rows = rows();
        for (var pair : List.of(List.of(owner, friend), List.of(friend, owner))) {
            jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", pair.get(0).id, pair.get(1).id);
            assertThat(upload(owner.token, jpeg(), "", friend.id.toString()).statusCode()).isEqualTo(400);
            jdbc.update("DELETE FROM blocks WHERE blocker_id=? AND blocked_id=?", pair.get(0).id, pair.get(1).id);
        }
        jdbc.update("DELETE FROM friendships WHERE user_low_id=least(?,?) AND user_high_id=greatest(?,?)", owner.id, friend.id, owner.id, friend.id);
        assertThat(upload(owner.token, jpeg(), "", friend.id.toString()).statusCode()).isEqualTo(400);
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
    }

    @Test void recipientInsertFailureRollsBackMomentRecipientsAndPrivateFile() throws Exception {
        var owner = account(); var first = account(); var second = account(); befriend(owner, first); befriend(owner, second);
        long files = fileCount(); int rows = rows();
        int recipients = jdbc.queryForObject("SELECT count(*) FROM moment_recipients", Integer.class);
        jdbc.execute("CREATE FUNCTION qa_reject_recipient() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'qa rollback'; END; $$");
        jdbc.execute("CREATE TRIGGER qa_reject_recipient BEFORE INSERT ON moment_recipients FOR EACH ROW EXECUTE FUNCTION qa_reject_recipient()");
        try { assertThat(upload(owner.token, jpeg(), "", first.id.toString(), second.id.toString()).statusCode()).isEqualTo(500); }
        finally { jdbc.execute("DROP TRIGGER qa_reject_recipient ON moment_recipients"); jdbc.execute("DROP FUNCTION qa_reject_recipient()"); }
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM moment_recipients", Integer.class)).isEqualTo(recipients);
    }

    @Test void uploadWaitsForRelationshipLockAndRevalidatesAfterBlock() throws Exception {
        var owner = account(); var friend = account(); befriend(owner, friend);
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<HttpResponse<String>>>();
        long files = fileCount(); int rows = rows();
        try {
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                context.getBean(com.taktak.server.user.UserRepository.class).lockPair(owner.id, friend.id);
                future.set(pool.submit(() -> upload(owner.token, jpeg(), "", friend.id.toString())));
                long deadline = System.nanoTime() + java.time.Duration.ofSeconds(10).toNanos(); boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    jdbc.execute("select pg_stat_clear_snapshot()");
                    waiting = Boolean.TRUE.equals(jdbc.queryForObject("SELECT exists(SELECT 1 FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE '%order by id for update%')", Boolean.class));
                    if (waiting) { break; }
                    try { Thread.sleep(30); } catch (InterruptedException failure) { throw new AssertionError(failure); }
                }
                assertThat(waiting).isTrue();
                jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)", friend.id, owner.id);
            });
            assertThat(future.get().get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode()).isEqualTo(400);
            assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
        } finally { pool.shutdownNow(); }
    }

    private void befriend(Account first, Account second) {
        jdbc.update("INSERT INTO friendships(user_low_id,user_high_id) VALUES(least(?,?),greatest(?,?))", first.id, second.id, first.id, second.id);
    }

    @Test void acceptsExactlyOneHundredRecipientsWithoutContainerTruncation() throws Exception {
        var owner = account(); var recipients = new ArrayList<String>();
        for (int index = 0; index < 100; index++) {
            var peer = account(); befriend(owner, peer); recipients.add(peer.id.toString());
        }
        var response = upload(owner.token, jpeg(), "100", recipients.toArray(String[]::new));
        assertThat(response.statusCode()).isEqualTo(201);
        UUID id = UUID.fromString(json.readTree(response.body()).get("id").asText());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM moment_recipients WHERE moment_id=?", Integer.class, id)).isEqualTo(100);
    }

    @Test void overlappingMultiRecipientUploadsUseOneGlobalLockOrder() throws Exception {
        var first = account(); var second = account(); var third = account();
        befriend(first, second); befriend(first, third); befriend(second, third);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = executor.submit(() -> { start.await(); return upload(first.token, jpeg(), "", third.id.toString(), second.id.toString()).statusCode(); });
            var b = executor.submit(() -> { start.await(); return upload(second.token, jpeg(), "", first.id.toString(), third.id.toString()).statusCode(); });
            start.countDown();
            assertThat(a.get(20, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(201);
            assertThat(b.get(20, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(201);
        } finally { executor.shutdownNow(); }
    }

    @Test void mimeMustMatchJpegAndActualExifIsRemoved() throws Exception {
        var service = context.getBean(PrivateImageStorage.class);
        byte[] jpeg = jpeg();
        assertThatThrownBy(() -> service.sanitize(new org.springframework.mock.web.MockMultipartFile("image", "fake.jpg", "image/png", jpeg)))
                .isInstanceOf(com.taktak.server.moment.MomentFailure.class);
        byte[] exif = "Exif\0\0private-gps-marker".getBytes(StandardCharsets.ISO_8859_1);
        var input = new ByteArrayOutputStream(); input.write(jpeg, 0, 2);
        input.write(255); input.write(225); input.write((exif.length + 2) >> 8); input.write((exif.length + 2) & 255);
        input.write(exif); input.write(jpeg, 2, jpeg.length - 2);
        var owner = account(); var response = upload(owner.token, input.toByteArray(), "");
        assertThat(response.statusCode()).isEqualTo(201);
        var result = image(owner.token, UUID.fromString(json.readTree(response.body()).get("id").asText()));
        assertThat(new String(result.body(), StandardCharsets.ISO_8859_1)).doesNotContain("Exif", "private-gps-marker");
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(result.body()))).isNotNull();
    }

    @Test void rejectsFakeJpegAndOversizeAndMalformedMultipartWithoutGarbage() throws Exception {
        var owner = account(); long files = fileCount(); int rows = rows();
        assertThat(upload(owner.token, "not-a-jpeg".getBytes(StandardCharsets.UTF_8), "", "[]").statusCode()).isEqualTo(415);
        assertThat(upload(owner.token, new byte[]{(byte)255, (byte)216, (byte)255, 0}, "", "[]").statusCode()).isEqualTo(415);
        assertThat(upload(owner.token, new byte[PrivateImageStorage.MAX_BYTES + 1], "", "[]").statusCode()).isEqualTo(413);
        var missing = client.send(HttpRequest.newBuilder(URI.create(base + "/api/moments"))
                .header("Authorization", "Bearer " + owner.token).header("Content-Type", "multipart/form-data; boundary=missing")
                .POST(HttpRequest.BodyPublishers.ofString("--missing--\r\n")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(missing.statusCode()).isEqualTo(400);
        assertThat(image(owner.token, UUID.randomUUID()).statusCode()).isEqualTo(404);
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
    }

    @Test void databaseFailureRollsBackAndDiscardsWrittenPrivateImage() throws Exception {
        var owner = account(); long files = fileCount(); int rows = rows();
        jdbc.execute("CREATE FUNCTION qa_reject_moment() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'qa rollback'; END; $$");
        jdbc.execute("CREATE TRIGGER qa_reject_moment BEFORE INSERT ON moments FOR EACH ROW EXECUTE FUNCTION qa_reject_moment()");
        try { assertThat(upload(owner.token, jpeg(), "", "[]").statusCode()).isEqualTo(500); }
        finally { jdbc.execute("DROP TRIGGER qa_reject_moment ON moments"); jdbc.execute("DROP FUNCTION qa_reject_moment()"); }
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
        assertThat(upload(owner.token, jpeg(), "retry", "[]").statusCode()).isEqualTo(201);
    }

    @Test void oversizedPixelDimensionsAreRejectedBeforeBitmapAllocation() throws Exception {
        var owner = account(); byte[] malicious = jpeg(); long files = fileCount(); int rows = rows();
        boolean patched = false;
        for (int i = 0; i + 9 < malicious.length; i++) {
            if ((malicious[i] & 255) == 255 && (malicious[i + 1] & 255) == 192) {
                malicious[i + 5] = (byte)255; malicious[i + 6] = (byte)255;
                malicious[i + 7] = (byte)255; malicious[i + 8] = (byte)255; patched = true; break;
            }
        }
        assertThat(patched).isTrue();
        assertThat(upload(owner.token, malicious, "", "[]").statusCode()).isEqualTo(415);
        assertThat(fileCount()).isEqualTo(files); assertThat(rows()).isEqualTo(rows);
    }

    @Test void deletedMomentIsDeniedOnNextRequestAndMissingFileIsSafe() throws Exception {
        var owner = account(); UUID id = uploaded(owner.token);
        assertThat(image(owner.token, id).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE moments SET deleted_at=CURRENT_TIMESTAMP WHERE id=?", id);
        assertThat(image(owner.token, id).statusCode()).isEqualTo(404);
        UUID missing = uploaded(owner.token);
        // Chỉ xóa file fixture vừa tạo trong thư mục test riêng, không phải dữ liệu người dùng.
        Files.delete(storage.resolve(missing + ".jpg"));
        assertThat(image(owner.token, missing).statusCode()).isEqualTo(404);
    }

    private record Account(UUID id, String username, String token) { }
    private Account account() {
        UUID id = UUID.randomUUID(); String suffix = id.toString().replace("-", "");
        String username = "media_" + suffix;
        jdbc.update("INSERT INTO users(id,email,username,password_hash,display_name) VALUES(?,?,?,?,?)",
                id, suffix + "@example.test", username, "fixture-not-login", "Media QA");
        String token = new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status ->
                context.getBean(TokenService.class).issue(id).accessToken());
        return new Account(id, username, token);
    }
    private UUID uploaded(String token) throws Exception {
        var response = upload(token, jpeg(), "", "[]");
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(json.readTree(response.body()).get("id").asText());
    }
    @Test void idempotentUploadReturnsSameMomentIdAndDoesNotDuplicateFilesOrRows() throws Exception {
        var owner = account();
        String key = UUID.randomUUID().toString();
        byte[] jpeg = jpeg();
        long initialFiles = fileCount(); int initialRows = rows();

        var first = uploadWithKey(owner.token, key, jpeg, "Hello Idempotent", "[]");
        assertThat(first.statusCode()).isEqualTo(201);
        UUID id1 = UUID.fromString(json.readTree(first.body()).get("id").asText());
        assertThat(fileCount()).isEqualTo(initialFiles + 1);
        assertThat(rows()).isEqualTo(initialRows + 1);

        // Retry với cùng key và payload
        var retry = uploadWithKey(owner.token, key, jpeg, "Hello Idempotent", "[]");
        assertThat(retry.statusCode()).isEqualTo(201);
        UUID id2 = UUID.fromString(json.readTree(retry.body()).get("id").asText());
        assertThat(id2).isEqualTo(id1);
        // Không tạo file mới, không tạo row mới
        assertThat(fileCount()).isEqualTo(initialFiles + 1);
        assertThat(rows()).isEqualTo(initialRows + 1);
    }

    @Test void idempotencyConflictOnDifferentPayload() throws Exception {
        var owner = account();
        String key = UUID.randomUUID().toString();
        byte[] jpeg = jpeg();
        var first = uploadWithKey(owner.token, key, jpeg, "Original Caption", "[]");
        assertThat(first.statusCode()).isEqualTo(201);

        // Khác caption
        var conflictCaption = uploadWithKey(owner.token, key, jpeg, "Different Caption", "[]");
        assertThat(conflictCaption.statusCode()).isEqualTo(409);
        assertThat(conflictCaption.body()).contains("IDEMPOTENCY_CONFLICT");

        // Khác image
        var differentImage = new BufferedImage(90, 90, BufferedImage.TYPE_INT_RGB);
        var diffOut = new ByteArrayOutputStream(); ImageIO.write(differentImage, "jpeg", diffOut);
        var conflictImage = uploadWithKey(owner.token, key, diffOut.toByteArray(), "Original Caption", "[]");
        assertThat(conflictImage.statusCode()).isEqualTo(409);
        assertThat(conflictImage.body()).contains("IDEMPOTENCY_CONFLICT");
    }

    @Test void invalidIdempotencyKeyRejected() throws Exception {
        var owner = account();
        var response = uploadWithKey(owner.token, "not-a-valid-uuid", jpeg(), "Test", "[]");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("INVALID_IDEMPOTENCY_KEY");
    }

    @Test void concurrentUploadWithSameIdempotencyKeyReturnsSameIdWithoutDuplicates() throws Exception {
        var owner = account();
        String key = UUID.randomUUID().toString();
        byte[] jpeg = jpeg();
        long initialFiles = fileCount(); int initialRows = rows();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = executor.submit(() -> { start.await(); return uploadWithKey(owner.token, key, jpeg, "Concurrent", "[]"); });
            var b = executor.submit(() -> { start.await(); return uploadWithKey(owner.token, key, jpeg, "Concurrent", "[]"); });
            start.countDown();
            var resA = a.get(10, java.util.concurrent.TimeUnit.SECONDS);
            var resB = b.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(resA.statusCode()).isEqualTo(201);
            assertThat(resB.statusCode()).isEqualTo(201);
            UUID idA = UUID.fromString(json.readTree(resA.body()).get("id").asText());
            UUID idB = UUID.fromString(json.readTree(resB.body()).get("id").asText());
            assertThat(idA).isEqualTo(idB);
            assertThat(fileCount()).isEqualTo(initialFiles + 1);
            assertThat(rows()).isEqualTo(initialRows + 1);
        } finally { executor.shutdownNow(); }
    }

    @Test void listMomentsReturnsEmptyWhenNoMomentsReceived() throws Exception {
        var user = account();
        var response = listMoments(user.token, "received", 0);
        assertThat(response.statusCode()).isEqualTo(200);
        var tree = json.readTree(response.body());
        assertThat(tree.get("items").size()).isEqualTo(0);
        assertThat(tree.get("page").asInt()).isEqualTo(0);
        assertThat(tree.get("hasNext").asBoolean()).isFalse();
    }

    @Test void listMomentsReturnsReceivedMomentsForRecipientAndNotForStranger() throws Exception {
        var sender = account(); var recipient = account(); var stranger = account();
        befriend(sender, recipient); befriend(sender, stranger);
        var uploadRes = upload(sender.token, jpeg(), "Hello 5E", recipient.id.toString());
        assertThat(uploadRes.statusCode()).isEqualTo(201);
        UUID momentId = UUID.fromString(json.readTree(uploadRes.body()).get("id").asText());

        var recipientRes = listMoments(recipient.token, "received", 0);
        assertThat(recipientRes.statusCode()).isEqualTo(200);
        var tree = json.readTree(recipientRes.body());
        assertThat(tree.get("items").size()).isEqualTo(1);
        var item = tree.get("items").get(0);
        assertThat(item.get("id").asText()).isEqualTo(momentId.toString());
        assertThat(item.get("caption").asText()).isEqualTo("Hello 5E");
        assertThat(item.get("sender").get("id").asText()).isEqualTo(sender.id.toString());
        assertThat(item.get("sender").get("username").asText()).isEqualTo(sender.username);

        var strangerRes = listMoments(stranger.token, "received", 0);
        assertThat(strangerRes.statusCode()).isEqualTo(200);
        assertThat(json.readTree(strangerRes.body()).get("items").size()).isEqualTo(0);
    }

    @Test void listMomentsExcludesOwnerOnlyPrivateMomentsFromReceivedTab() throws Exception {
        var owner = account();
        var uploadRes = upload(owner.token, jpeg(), "Private self moment");
        assertThat(uploadRes.statusCode()).isEqualTo(201);

        var listRes = listMoments(owner.token, "received", 0);
        assertThat(listRes.statusCode()).isEqualTo(200);
        assertThat(json.readTree(listRes.body()).get("items").size()).isEqualTo(0);
    }

    @Test void listMomentsSupportsPagination() throws Exception {
        var sender = account(); var recipient = account();
        befriend(sender, recipient);
        for (int i = 0; i < 21; i++) {
            var res = upload(sender.token, jpeg(), "Moment " + i, recipient.id.toString());
            assertThat(res.statusCode()).isEqualTo(201);
        }
        var page0 = listMoments(recipient.token, "received", 0);
        assertThat(page0.statusCode()).isEqualTo(200);
        var tree0 = json.readTree(page0.body());
        assertThat(tree0.get("items").size()).isEqualTo(20);
        assertThat(tree0.get("hasNext").asBoolean()).isTrue();
        assertThat(tree0.get("page").asInt()).isEqualTo(0);

        var page1 = listMoments(recipient.token, "received", 1);
        assertThat(page1.statusCode()).isEqualTo(200);
        var tree1 = json.readTree(page1.body());
        assertThat(tree1.get("items").size()).isEqualTo(1);
        assertThat(tree1.get("hasNext").asBoolean()).isFalse();
        assertThat(tree1.get("page").asInt()).isEqualTo(1);
    }

    @Test void listMomentsRejectsInvalidTypeOrNegativePage() throws Exception {
        var user = account();
        assertThat(listMoments(user.token, "unknown", 0).statusCode()).isEqualTo(400);
        assertThat(listMoments(user.token, "received", -1).statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> listMoments(String token, String type, int page) throws Exception {
        String uri = base + "/api/moments?type=" + (type != null ? type : "") + "&page=" + page;
        var request = HttpRequest.newBuilder(URI.create(uri));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> upload(String token, byte[] image, String caption, String... recipients) throws Exception {
        return uploadWithKey(token, null, image, caption, recipients);
    }
    private HttpResponse<String> uploadWithKey(String token, String idempotencyKey, byte[] image, String caption, String... recipients) throws Exception {
        String boundary = "Taktak" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n" + caption).getBytes(StandardCharsets.UTF_8));
        for (String recipient : recipients) {
            body.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"recipientIds\"\r\n\r\n" + recipient).getBytes(StandardCharsets.UTF_8));
        }
        body.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"../../untrusted.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image); body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments"))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        if (idempotencyKey != null) { request.header("Idempotency-Key", idempotencyKey); }
        return client.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<byte[]> image(String token, UUID id) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments/" + id + "/image"));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    private byte[] jpeg() throws Exception {
        var image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream(); ImageIO.write(image, "jpeg", output); image.flush(); return output.toByteArray();
    }
    private long fileCount() throws Exception { try (var files = Files.list(storage)) { return files.count(); } }
    private int rows() { return jdbc.queryForObject("SELECT count(*) FROM moments", Integer.class); }
}
