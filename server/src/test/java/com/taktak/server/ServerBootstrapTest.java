package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerBootstrapTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

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
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
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
                .isEqualTo(1);
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
