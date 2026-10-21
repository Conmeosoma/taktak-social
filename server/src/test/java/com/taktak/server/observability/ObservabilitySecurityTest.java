package com.taktak.server.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.TaktakApplication;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObservabilitySecurityTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "observability-postgres-tests"));
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        context = (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key),
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--server.address=127.0.0.1", "--server.port=0", "--taktak.fcm.enabled=false",
                "--debug=false", "--logging.level.root=WARN");
        base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll void stop() throws Exception {
        try { if (context != null) context.close(); }
        finally { if (postgres != null) postgres.close(); }
    }

    @Test void publicHealthAndAuthenticationRejectionBothReturnServerRequestId() throws Exception {
        assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health");
        assertThat(((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
                "org.hibernate.engine.jdbc.spi.SqlExceptionHelper")).getEffectiveLevel())
                .isEqualTo(ch.qos.logback.classic.Level.OFF);
        for (String path : List.of("/actuator/health", "/api/users/me")) {
            var response = get(path, null);
            assertThat(response.statusCode()).isEqualTo(path.endsWith("health") ? 200 : 401);
            assertThat(UUID.fromString(response.headers().firstValue("X-Request-ID").orElseThrow())).isNotNull();
            assertThat(response.headers().firstValue("X-Request-ID").orElseThrow()).isNotEqualTo("client-controlled");
        }
    }

    @Test void operationalEndpointsAreClosedToAnonymousAndOrdinaryAuthenticatedUser() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        String email = suffix + "@example.test";
        assertThat(post("/api/auth/register", Map.of("email", email, "username", "obs" + suffix,
                "displayName", "QA", "password", password)).statusCode()).isEqualTo(201);
        var login = post("/api/auth/login", Map.of("email", email, "password", password));
        assertThat(login.statusCode()).isEqualTo(200);
        String token = json.readTree(login.body()).get("accessToken").asText();
        assertThat(get("/api/users/me", token).statusCode()).isEqualTo(200);
        for (String endpoint : List.of("/actuator/metrics", "/actuator/env", "/actuator/configprops",
                "/actuator/prometheus", "/actuator/health/db")) {
            assertThat(get(endpoint, null).statusCode()).as(endpoint).isEqualTo(401);
            assertThat(get(endpoint, token).statusCode()).as(endpoint).isEqualTo(403);
        }
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).header("X-Request-ID", "client-controlled");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, String> payload) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
