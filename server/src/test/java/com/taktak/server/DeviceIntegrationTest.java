package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeviceIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "device-tests"));
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key),
                "--server.port=0", "--server.address=127.0.0.1", "--debug=false", "--logging.level.root=WARN");
        jdbc = context.getBean(JdbcTemplate.class);
        base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll
    void stop() throws Exception {
        try { if (context != null) { context.close(); } }
        finally { if (postgres != null) { postgres.close(); } }
    }

    private record Account(UUID id, String token) { }

    private Account account() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String body = String.format("{\"email\":\"%s@example.test\",\"username\":\"%s\",\"displayName\":\"User\",\"password\":\"Password123\"}",
                username, username);
        var res = client.send(HttpRequest.newBuilder().uri(URI.create(base + "/api/auth/register"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(201);
        UUID id = UUID.fromString(json.readTree(res.body()).get("id").asText());
        String token = context.getBean(TokenService.class).issue(id).accessToken();
        return new Account(id, token);
    }

    private HttpResponse<String> registerDevice(String token, String deviceId, String fcmToken) throws Exception {
        String payload = String.format("{\"deviceId\":\"%s\",\"fcmToken\":\"%s\"}", deviceId, fcmToken);
        var req = HttpRequest.newBuilder().uri(URI.create(base + "/api/devices"))
                .header("Content-Type", "application/json");
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return client.send(req.POST(HttpRequest.BodyPublishers.ofString(payload)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> unregisterDevice(String token, String deviceId) throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(base + "/api/devices/" + deviceId));
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return client.send(req.DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> listDevices(String token) throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(base + "/api/devices"));
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return client.send(req.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registerDeviceSavesTokenAndReturns204() throws Exception {
        var acc = account();
        String deviceId = "dev-" + UUID.randomUUID();
        String fcmToken = "fcm-" + UUID.randomUUID();

        var res = registerDevice(acc.token, deviceId, fcmToken);
        assertThat(res.statusCode()).isEqualTo(204);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ? AND device_id = ? AND fcm_token = ?",
                Integer.class, acc.id, deviceId, fcmToken);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void reRegisterSameDeviceUpdatesToken() throws Exception {
        var acc = account();
        String deviceId = "dev-" + UUID.randomUUID();
        String oldToken = "fcm-old-" + UUID.randomUUID();
        String newToken = "fcm-new-" + UUID.randomUUID();

        assertThat(registerDevice(acc.token, deviceId, oldToken).statusCode()).isEqualTo(204);
        assertThat(registerDevice(acc.token, deviceId, newToken).statusCode()).isEqualTo(204);

        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ? AND device_id = ?",
                Integer.class, acc.id, deviceId);
        assertThat(total).isEqualTo(1);

        String currentToken = jdbc.queryForObject(
                "SELECT fcm_token FROM device_tokens WHERE user_id = ? AND device_id = ?",
                String.class, acc.id, deviceId);
        assertThat(currentToken).isEqualTo(newToken);
    }

    @Test
    void sameTokenOnDifferentUserTransfersTokenCleanly() throws Exception {
        var user1 = account();
        var user2 = account();
        String sharedToken = "fcm-shared-" + UUID.randomUUID();

        assertThat(registerDevice(user1.token, "device-phone", sharedToken).statusCode()).isEqualTo(204);
        assertThat(registerDevice(user2.token, "device-tablet", sharedToken).statusCode()).isEqualTo(204);

        // Token must now belong to user2, and user1's record must be cleaned up to satisfy UNIQUE constraint
        Integer user1Count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ? AND fcm_token = ?",
                Integer.class, user1.id, sharedToken);
        assertThat(user1Count).isEqualTo(0);

        Integer user2Count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ? AND fcm_token = ?",
                Integer.class, user2.id, sharedToken);
        assertThat(user2Count).isEqualTo(1);
    }

    @Test
    void multiDeviceForSameUserKeepsBothDevices() throws Exception {
        var acc = account();
        String dev1 = "dev-phone-" + UUID.randomUUID();
        String dev2 = "dev-tablet-" + UUID.randomUUID();
        String token1 = "token-1-" + UUID.randomUUID();
        String token2 = "token-2-" + UUID.randomUUID();

        assertThat(registerDevice(acc.token, dev1, token1).statusCode()).isEqualTo(204);
        assertThat(registerDevice(acc.token, dev2, token2).statusCode()).isEqualTo(204);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ?",
                Integer.class, acc.id);
        assertThat(count).isEqualTo(2);

        var listRes = listDevices(acc.token);
        assertThat(listRes.statusCode()).isEqualTo(200);
        var array = json.readTree(listRes.body());
        assertThat(array.size()).isEqualTo(2);
    }

    @Test
    void unregisterDeviceRemovesTokenFromDatabase() throws Exception {
        var acc = account();
        String deviceId = "dev-" + UUID.randomUUID();
        String token = "token-" + UUID.randomUUID();

        assertThat(registerDevice(acc.token, deviceId, token).statusCode()).isEqualTo(204);
        assertThat(unregisterDevice(acc.token, deviceId).statusCode()).isEqualTo(204);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE user_id = ? AND device_id = ?",
                Integer.class, acc.id, deviceId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void registerDeviceWithoutAuthReturns401() throws Exception {
        var res = registerDevice(null, "dev-1", "fcm-1");
        assertThat(res.statusCode()).isEqualTo(401);
    }

    @Test
    void registerDeviceWithBlankFieldsReturns400() throws Exception {
        var acc = account();
        var resBlankDevice = registerDevice(acc.token, "  ", "fcm-valid");
        assertThat(resBlankDevice.statusCode()).isEqualTo(400);

        var resBlankToken = registerDevice(acc.token, "dev-valid", "  ");
        assertThat(resBlankToken.statusCode()).isEqualTo(400);
    }
}
