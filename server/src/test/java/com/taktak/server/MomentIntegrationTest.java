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

    private record Account(UUID id, String token) { }
    private Account account() {
        UUID id = UUID.randomUUID(); String suffix = id.toString().replace("-", "");
        jdbc.update("INSERT INTO users(id,email,username,password_hash,display_name) VALUES(?,?,?,?,?)",
                id, suffix + "@example.test", "media_" + suffix, "fixture-not-login", "Media QA");
        String token = new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status ->
                context.getBean(TokenService.class).issue(id).accessToken());
        return new Account(id, token);
    }
    private UUID uploaded(String token) throws Exception {
        var response = upload(token, jpeg(), "", "[]");
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(json.readTree(response.body()).get("id").asText());
    }
    private HttpResponse<String> upload(String token, byte[] image, String caption, String recipients) throws Exception {
        String boundary = "Taktak" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n" + caption
                + "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"recipientIds\"\r\n\r\n" + recipients
                + "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"../../untrusted.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image); body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(base + "/api/moments"))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) { request.header("Authorization", "Bearer " + token); }
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
