package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.RegisterRequest;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerBootstrapTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll
    void startServerWithIsolatedPostgres() throws Exception {
        // Chỉ tạo cluster test mới, không dùng DB_URL hoặc database của người dùng.
        Path testRoot = Files.createDirectories(Path.of("target", "postgres-tests"));
        postgres = EmbeddedPostgres.builder()
                .setDataDirectory(Files.createTempDirectory(testRoot, "cluster-"))
                .setOverrideWorkingDirectory(testRoot.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1")
                .setPort(0)
                .start();
        byte[] testKey = new byte[32];
        new java.security.SecureRandom().nextBytes(testKey);
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                "--taktak.jwt.access-secret=" + java.util.Base64.getEncoder().encodeToString(testKey),
                "--taktak.jwt.access-minutes=15", "--taktak.jwt.refresh-days=30",
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres",
                "--spring.datasource.password=",
                "--server.address=127.0.0.1",
                "--server.port=0",
                "--debug=false",
                "--logging.level.root=WARN");
        jdbc = context.getBean(JdbcTemplate.class);
        baseUrl = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll
    void stopServerAndTestDatabase() throws Exception {
        try {
            if (context != null) {
                context.close();
            }
        } finally {
            if (postgres != null) {
                postgres.close();
            }
        }
    }

    @Test
    void healthIsPublicAndDoesNotExposeDatabaseDetails() throws Exception {
        var response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new ObjectMapper().readTree(response.body()).toString()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
    }

    @Test
    void otherEndpointsAreClosedWithoutLoginRedirectOrDefaultBasicAuth() throws Exception {
        for (String path : List.of("/api/users/me", "/actuator/env", "/actuator/health/db", "/private-images/test.jpg")) {
            var response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(response.headers().allValues("location")).isEmpty();
            assertThat(response.headers().allValues("www-authenticate")).isEmpty();
        }
    }

    @Test
    void migrationCreatesEightTablesIndexesAndIsRepeatableWithoutReapplying() {
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """, String.class)).containsExactly("blocks", "device_tokens", "friend_requests", "friendships",
                "moment_recipients", "moments", "refresh_tokens", "users");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = ?", String.class,
                "uq_moments_sender_idempotency")).contains("sender_id, idempotency_key");
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = ?", String.class,
                "ix_moment_recipients_user_created")).contains("user_id, created_at DESC");
        assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
    }

    @Test
    void usersCannotDuplicateEmailCaseInsensitivelyOrUsername() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        addUser("owner_" + suffix, "Owner" + suffix + "@example.test");
        assertThatThrownBy(() -> addUser("other_" + suffix, "owner" + suffix + "@example.test"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> addUser("owner_" + suffix, "different" + suffix + "@example.test"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void friendshipsAndPendingRequestsPreventReverseDuplicatesAndSelfRelationships() {
        UUID first = addUser();
        UUID second = addUser();
        // PostgreSQL UUID dùng thứ tự unsigned; không dùng UUID.compareTo của Java để chuẩn hóa.
        jdbc.update("INSERT INTO friendships (user_low_id, user_high_id) VALUES (LEAST(?, ?), GREATEST(?, ?))",
                first, second, first, second);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO friendships (user_low_id, user_high_id) VALUES (LEAST(?, ?), GREATEST(?, ?))",
                second, first, second, first)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO friendships (user_low_id, user_high_id) VALUES (?, ?)",
                first, first)).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("INSERT INTO friend_requests (sender_id, receiver_id) VALUES (?, ?)", first, second);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO friend_requests (sender_id, receiver_id) VALUES (?, ?)",
                second, first)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO blocks (blocker_id, blocked_id) VALUES (?, ?)", first, first))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void momentsEnforceForeignKeysPositiveSizeAndFiftyUnicodeCharacters() {
        UUID owner = addUser();
        String caption = "📷".repeat(50);
        UUID moment = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO moments (id, sender_id, caption, file_path, mime_type, byte_size)
                VALUES (?, ?, ?, ?, 'image/jpeg', 512)
                """, moment, owner, caption, "test/" + moment + ".jpg");
        assertThat(jdbc.queryForObject("SELECT char_length(caption) FROM moments WHERE id = ?", Integer.class, moment))
                .isEqualTo(50);
        assertThatThrownBy(() -> jdbc.update("UPDATE moments SET caption = ? WHERE id = ?", caption + "📷", moment))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE moments SET byte_size = 0 WHERE id = ?", moment))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO moment_recipients (moment_id, user_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, moment, UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registrationNormalizesIdentityHashesPasswordAndReturnsOnlySafeFields() throws Exception {
        String username = "member_" + suffix();
        String email = username + "@example.test";
        String password = "  " + UUID.randomUUID() + "  ";
        var body = registration(username.toUpperCase(java.util.Locale.ROOT), password);
        body.put("email", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ");
        body.put("username", "  " + username.toUpperCase(java.util.Locale.ROOT) + "  ");
        body.put("displayName", "  Bạn Taktak  ");
        body.put("id", UUID.randomUUID().toString());
        body.put("passwordHash", "untrusted-client-value");
        var response = postRegistration(body);
        assertThat(response.statusCode()).isEqualTo(201);
        var json = new ObjectMapper().readTree(response.body());
        assertThat(json.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("id", "email", "username", "displayName", "createdAt");
        UUID id = UUID.fromString(json.get("id").asText());
        assertThat(id.toString()).isNotEqualTo(body.get("id"));
        assertThat(json.get("email").asText()).isEqualTo(email);
        assertThat(json.get("username").asText()).isEqualTo(username);
        assertThat(json.get("displayName").asText()).isEqualTo("Bạn Taktak");
        assertThat(java.time.Instant.parse(json.get("createdAt").asText())).isNotNull();
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, id);
        assertThat(hash).startsWith("$2a$12$").isNotEqualTo(password);
        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches(password.strip(), hash)).isFalse();
        assertThat(jdbc.queryForObject("SELECT avatar_path FROM users WHERE id = ?", String.class, id)).isNull();
        assertThat(new RegisterRequest(email, username, "Name", password).toString()).isEqualTo("RegisterRequest[redacted]");
    }

    @Test
    void duplicateUsernameAndCaseInsensitiveEmailReturnConflict() throws Exception {
        String username = "duplicate_" + suffix();
        var first = registration(username, UUID.randomUUID().toString());
        assertThat(postRegistration(first).statusCode()).isEqualTo(201);
        var sameUsername = registration(username.toUpperCase(java.util.Locale.ROOT), UUID.randomUUID().toString());
        sameUsername.put("email", "other_" + suffix() + "@example.test");
        assertApiError(postRegistration(sameUsername), 409, "ACCOUNT_CONFLICT");
        var sameEmail = registration("other_" + suffix(), UUID.randomUUID().toString());
        sameEmail.put("email", first.get("email").toString().toUpperCase(java.util.Locale.ROOT));
        assertApiError(postRegistration(sameEmail), 409, "ACCOUNT_CONFLICT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE username = ? OR lower(email) = ?",
                Integer.class, username, first.get("email"))).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateUsernamesCreateExactlyOneCommittedUser() throws Exception {
        String username = "race_" + suffix();
        var first = registration(username, UUID.randomUUID().toString());
        var second = registration(username.toUpperCase(java.util.Locale.ROOT), UUID.randomUUID().toString());
        second.put("email", "other_" + suffix() + "@example.test");
        assertConcurrentConflict(first, second);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE username = ?", Integer.class, username))
                .isEqualTo(1);
    }

    @Test
    void concurrentCaseInsensitiveEmailsCreateExactlyOneCommittedUser() throws Exception {
        var first = registration("email_" + suffix(), UUID.randomUUID().toString());
        var second = registration("other_" + suffix(), UUID.randomUUID().toString());
        second.put("email", first.get("email").toString().toUpperCase(java.util.Locale.ROOT));
        assertConcurrentConflict(first, second);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(email) = ?",
                Integer.class, first.get("email"))).isEqualTo(1);
    }

    @Test
    void invalidRegistrationDoesNotInsertRowsOrReflectInput() throws Exception {
        int before = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
        var valid = registration("validate_" + suffix(), UUID.randomUUID().toString());
        for (String field : List.of("email", "username", "displayName", "password")) {
            for (Object invalid : new Object[]{null, "", "   "}) {
                Map<String, Object> body = new HashMap<>(valid);
                body.put(field, invalid);
                assertApiError(postRegistration(body), 400, "VALIDATION_ERROR");
            }
        }
        for (var invalid : List.of(Map.entry("email", "not-an-email"),
                Map.entry("email", "x".repeat(255) + "@example.test"),
                Map.entry("username", "invalid-name"), Map.entry("username", "é_user"),
                Map.entry("username", "x".repeat(256)), Map.entry("displayName", "x".repeat(256)),
                Map.entry("password", "x".repeat(7)), Map.entry("password", "x".repeat(73)),
                Map.entry("password", "📷".repeat(19)), Map.entry("password", "📷".repeat(7)),
                Map.entry("password", UUID.randomUUID() + "\0"))) {
            Map<String, Object> body = new HashMap<>(valid);
            body.put(invalid.getKey(), invalid.getValue());
            assertApiError(postRegistration(body), 400, "VALIDATION_ERROR");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(before);
    }

    @Test
    void bcryptAcceptsSeventyTwoUtf8BytesAndUsesDifferentSalts() throws Exception {
        String password = "📷".repeat(18);
        var first = registration("boundary_" + suffix(), password);
        var second = registration("boundary_" + suffix(), password);
        assertThat(postRegistration(first).statusCode()).isEqualTo(201);
        assertThat(postRegistration(second).statusCode()).isEqualTo(201);
        String firstHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = ?", String.class,
                first.get("username"));
        String secondHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = ?", String.class,
                second.get("username"));
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(context.getBean(PasswordEncoder.class).matches(password, firstHash)).isTrue();
    }

    @Test
    void malformedJsonAndContentTypeHaveSafeErrorsWhileOtherRoutesStayClosed() throws Exception {
        for (String body : List.of("{", "null", "[]", "{\"password\":{}}")) {
            assertApiError(postRaw(body, "application/json"), 400, "VALIDATION_ERROR");
        }
        assertApiError(postRaw("unsupported", "text/plain"), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(get("/api/auth/register").statusCode()).isEqualTo(401);
        for (String path : List.of("/api/users/me", "/api/friends", "/api/moments")) {
            var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(401);
        }
    }

    private void assertConcurrentConflict(Map<String, Object> first, Map<String, Object> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var tasks = List.of(first, second).stream().map(body -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Race start timed out."); }
                return postRegistration(body);
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var responses = List.of(tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS));
            assertThat(responses.stream().map(HttpResponse::statusCode).toList()).containsExactlyInAnyOrder(201, 409);
            for (var response : responses) {
                if (response.statusCode() == 409) { assertApiError(response, 409, "ACCOUNT_CONFLICT"); }
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void assertApiError(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        var json = new ObjectMapper().readTree(response.body());
        assertThat(json.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("timestamp", "status", "code", "message", "path");
        assertThat(json.get("status").asInt()).isEqualTo(status);
        assertThat(json.get("code").asText()).isEqualTo(code);
        assertThat(json.get("path").asText()).isEqualTo("/api/auth/register");
        assertThat(java.time.Instant.parse(json.get("timestamp").asText())).isNotNull();
        assertThat(json.get("message").asText()).isIn("Dữ liệu yêu cầu không hợp lệ.",
                "Email hoặc username đã được sử dụng.", "Yêu cầu JSON.");
    }

    private Map<String, Object> registration(String username, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", username.toLowerCase(java.util.Locale.ROOT) + "@example.test");
        body.put("username", username);
        body.put("displayName", "Taktak member");
        body.put("password", password);
        return body;
    }

    private HttpResponse<String> postRegistration(Map<String, Object> body) throws Exception {
        return postRaw(new ObjectMapper().writeValueAsString(body), "application/json");
    }

    private HttpResponse<String> postRaw(String body, String contentType) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/auth/register"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private UUID addUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return addUser("user_" + suffix, suffix + "@example.test");
    }

    private UUID addUser(String username, String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, username, password_hash, display_name)
                VALUES (?, ?, ?, ?, 'Test user')
                """, id, email, username, "fixture-hash-not-a-login-credential");
        return id;
    }
}
