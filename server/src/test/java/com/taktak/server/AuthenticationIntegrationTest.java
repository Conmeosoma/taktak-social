package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.taktak.server.auth.TokenService;
import com.taktak.server.config.TokenConfiguration;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private MutableClock clock;
    private String baseUrl;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll
    void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "auth-postgres-tests"));
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start();
        var application = new SpringApplication(TaktakApplication.class, TestBeans.class);
        application.setAdditionalProfiles("auth-test");
        context = (ServletWebServerApplicationContext) application.run(
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(randomBytes()),
                "--taktak.jwt.access-minutes=15", "--taktak.jwt.refresh-days=30",
                "--server.address=127.0.0.1", "--server.port=0", "--debug=false", "--logging.level.root=WARN");
        jdbc = context.getBean(JdbcTemplate.class);
        clock = context.getBean(MutableClock.class);
        baseUrl = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll
    void stop() throws Exception {
        try { if (context != null) { context.close(); } }
        finally { if (postgres != null) { postgres.close(); } }
    }

    @BeforeEach
    void nextWindow() {
        clock.advance(Duration.ofMinutes(2));
    }

    @Test
    void loginIssuesVerifiedJwtAndStoresOnlyRefreshHash() throws Exception {
        var account = register();
        var response = post("/login", Map.of("email", "  " + account.email.toUpperCase(java.util.Locale.ROOT) + "  ",
                "password", account.password), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
        JsonNode pair = json.readTree(response.body());
        assertThat(pair.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("accessToken", "refreshToken", "expiresIn", "tokenType");
        assertThat(pair.get("expiresIn").asLong()).isEqualTo(900);
        assertThat(pair.get("tokenType").asText()).isEqualTo("Bearer");
        Jwt jwt = context.getBean(JwtDecoder.class).decode(pair.get("accessToken").asText());
        assertThat(jwt.getSubject()).isEqualTo(account.id.toString());
        assertThat(jwt.getIssuer().toString()).isEqualTo(TokenConfiguration.ISSUER);
        assertThat(jwt.getAudience()).contains(TokenConfiguration.AUDIENCE);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(900);
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        String raw = pair.get("refreshToken").asText();
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        assertThat(jdbc.queryForObject("SELECT token_hash FROM refresh_tokens WHERE user_id = ?", String.class, account.id))
                .isEqualTo(TokenService.hash(raw)).isNotEqualTo(raw);
        var protectedResponse = probe(pair.get("accessToken").asText());
        assertThat(protectedResponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(protectedResponse.body()).get("subject").asText()).isEqualTo(account.id.toString());
    }

    @Test
    void wrongPasswordAndUnknownUserReturnSameSafeErrorWithoutCreatingSession() throws Exception {
        var account = register();
        var wrong = post("/login", Map.of("email", account.email, "password", UUID.randomUUID().toString()), null);
        var unknown = post("/login", Map.of("email", UUID.randomUUID() + "@example.test", "password", account.password), null);
        assertError(wrong, 401, "UNAUTHORIZED");
        assertError(unknown, 401, "UNAUTHORIZED");
        assertThat(json.readTree(wrong.body()).get("message")).isEqualTo(json.readTree(unknown.body()).get("message"));
        assertThat(sessionCount(account.id, false)).isZero();
    }

    @Test
    void refreshWorksAfterAccessExpiryIncludingExpiredBearerHeaderAndRejectsReplay() throws Exception {
        var account = register();
        var pair = login(account);
        clock.advance(Duration.ofMinutes(15));
        assertError(probe(pair.get("accessToken").asText()), 401, "UNAUTHORIZED");
        var response = post("/refresh", refreshBody(pair), pair.get("accessToken").asText());
        assertThat(response.statusCode()).isEqualTo(200);
        var rotated = json.readTree(response.body());
        assertThat(rotated.get("accessToken")).isNotEqualTo(pair.get("accessToken"));
        assertThat(rotated.get("refreshToken")).isNotEqualTo(pair.get("refreshToken"));
        assertThat(probe(rotated.get("accessToken").asText()).statusCode()).isEqualTo(200);
        assertError(post("/refresh", refreshBody(pair), null), 401, "UNAUTHORIZED");
        assertThat(sessionCount(account.id, false)).isEqualTo(2);
        assertThat(sessionCount(account.id, true)).isEqualTo(1);
    }

    @Test
    void concurrentRefreshRotatesExactlyOnce() throws Exception {
        var account = register();
        var pair = login(account);
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var calls = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Race start timed out."); }
                return post("/refresh", refreshBody(pair), null);
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var responses = List.of(calls.get(0).get(20, TimeUnit.SECONDS), calls.get(1).get(20, TimeUnit.SECONDS));
            assertThat(responses.stream().map(HttpResponse::statusCode).toList()).containsExactlyInAnyOrder(200, 401);
            assertThat(sessionCount(account.id, false)).isEqualTo(2);
            assertThat(sessionCount(account.id, true)).isEqualTo(1);
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test
    void failedRotationRollsBackRevocation() throws Exception {
        var account = register();
        var pair = login(account);
        context.getBean(ControlledEncoder.class).failNext.set(true);
        assertError(post("/refresh", refreshBody(pair), null), 500, "INTERNAL_ERROR");
        assertThat(sessionCount(account.id, false)).isEqualTo(1);
        assertThat(sessionCount(account.id, true)).isEqualTo(1);
        assertThat(post("/refresh", refreshBody(pair), null).statusCode()).isEqualTo(200);
    }

    @Test
    void expiredUnknownAndRevokedRefreshCannotIssueTokens() throws Exception {
        var account = register();
        var pair = login(account);
        clock.advance(Duration.ofDays(31));
        assertError(post("/refresh", refreshBody(pair), null), 401, "UNAUTHORIZED");
        assertError(post("/refresh", Map.of("refreshToken", opaqueToken()), null), 401, "UNAUTHORIZED");
        assertThat(sessionCount(account.id, false)).isEqualTo(1);
    }

    @Test
    void logoutIsIdempotentRevokesRefreshButAccessLastsUntilItsExpiry() throws Exception {
        var account = register();
        var pair = login(account);
        assertThat(post("/logout", refreshBody(pair), "malformed-bearer").statusCode()).isEqualTo(204);
        assertThat(post("/logout", refreshBody(pair), null).statusCode()).isEqualTo(204);
        assertThat(post("/logout", Map.of("refreshToken", opaqueToken()), null).statusCode()).isEqualTo(204);
        assertError(post("/refresh", refreshBody(pair), null), 401, "UNAUTHORIZED");
        assertThat(probe(pair.get("accessToken").asText()).statusCode()).isEqualTo(200);
        clock.advance(Duration.ofMinutes(16));
        assertError(probe(pair.get("accessToken").asText()), 401, "UNAUTHORIZED");
    }

    @Test
    void logoutOnlyRevokesSelectedSession() throws Exception {
        var account = register();
        var first = login(account);
        var second = login(account);
        assertThat(post("/logout", refreshBody(first), null).statusCode()).isEqualTo(204);
        assertError(post("/refresh", refreshBody(first), null), 401, "UNAUTHORIZED");
        assertThat(post("/refresh", refreshBody(second), null).statusCode()).isEqualTo(200);
    }

    @Test
    void forgedMalformedWrongIssuerAudienceAndMissingExpiryAreRejected() throws Exception {
        var account = register();
        assertError(probe(null), 401, "UNAUTHORIZED");
        assertError(probe("invalid.jwt.token"), 401, "UNAUTHORIZED");
        for (String variant : List.of("wrong-key", "issuer", "audience", "expiry", "subject", "future-iat")) {
            JwtEncoder encoder = variant.equals("wrong-key")
                    ? new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(randomBytes(), "HmacSHA256")))
                    : context.getBean("jwtEncoder", JwtEncoder.class);
            Instant now = clock.instant();
            var claims = JwtClaimsSet.builder().subject(variant.equals("subject") ? "not-a-uuid" : account.id.toString())
                    .issuer(variant.equals("issuer") ? "another-server" : TokenConfiguration.ISSUER)
                    .audience(List.of(variant.equals("audience") ? "another-client" : TokenConfiguration.AUDIENCE))
                    .issuedAt(variant.equals("future-iat") ? now.plusSeconds(60) : now);
            if (!variant.equals("expiry")) { claims.expiresAt(now.plusSeconds(900)); }
            String jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                    .getTokenValue();
            assertError(probe(jwt), 401, "UNAUTHORIZED");
        }
        String validJwt = login(account).get("accessToken").asText();
        String unsigned = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "." + validJwt.split("\\.")[1] + ".";
        assertError(probe(unsigned), 401, "UNAUTHORIZED");
        assertError(get("/actuator/env", login(account).get("accessToken").asText()), 403, "FORBIDDEN");
    }

    @Test
    void accountRateLimitReturnsRetryAfterAndExpires() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        var credentials = Map.of("email", email, "password", UUID.randomUUID().toString());
        for (int index = 0; index < 5; index++) { assertError(post("/login", credentials, null), 401, "UNAUTHORIZED"); }
        var blocked = post("/login", credentials, null);
        assertError(blocked, 429, "LOGIN_RATE_LIMITED");
        assertThat(blocked.headers().firstValue("retry-after")).hasValue("60");
        clock.advance(Duration.ofSeconds(61));
        assertError(post("/login", credentials, null), 401, "UNAUTHORIZED");
    }

    @Test
    void ipRateLimitCannotBeBypassedWithForwardedHeader() throws Exception {
        for (int index = 0; index < 20; index++) {
            var credentials = Map.of("email", UUID.randomUUID() + "@example.test", "password", UUID.randomUUID().toString());
            assertError(send("POST", "/api/auth/login", json.writeValueAsString(credentials), null,
                    "203.0.113." + (index + 1)), 401, "UNAUTHORIZED");
        }
        assertError(send("POST", "/api/auth/login", json.writeValueAsString(Map.of(
                "email", UUID.randomUUID() + "@example.test", "password", UUID.randomUUID().toString())), null,
                "198.51.100.1"), 429, "LOGIN_RATE_LIMITED");
    }

    @Test
    void invalidBodiesNeverExposeSecretsOrCreateSessions() throws Exception {
        int before = jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class);
        for (String path : List.of("/login", "/refresh", "/logout")) {
            assertError(post(path, Map.of(), null), 400, "VALIDATION_ERROR");
            assertError(send("POST", "/api/auth" + path, "{", null, null), 400, "VALIDATION_ERROR");
        }
        assertError(post("/login", Map.of("email", "invalid", "password", "x".repeat(73)), null), 400, "VALIDATION_ERROR");
        assertError(post("/login", Map.of("email", "a@example.test", "password", "📷".repeat(19)), null), 400, "VALIDATION_ERROR");
        assertError(post("/refresh", Map.of("refreshToken", "not-a-refresh-token"), null), 400, "VALIDATION_ERROR");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class)).isEqualTo(before);
    }

    @Test
    void profileReturnsOnlySafeFieldsForAuthenticatedUser() throws Exception {
        var account = register();
        String bearer = login(account).get("accessToken").asText();
        var response = get("/api/users/me", bearer);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
        var profile = json.readTree(response.body());
        assertProfileFields(profile);
        assertThat(profile.get("id").asText()).isEqualTo(account.id.toString());
        assertThat(profile.get("email").asText()).isEqualTo(account.email);
        assertThat(profile.get("username").asText()).isEqualTo(account.email.split("@")[0]);
        assertThat(profile.get("displayName").asText()).isEqualTo("Test member");
        assertThat(Instant.parse(profile.get("createdAt").asText())).isNotNull();
    }

    @Test
    void profileUpdatePersistsTrimmedUnicodeNameAndIsIdempotent() throws Exception {
        var account = register();
        String bearer = login(account).get("accessToken").asText();
        var before = userRow(account.id);
        String name = "Tiến 📷 O'Brien";
        for (int index = 0; index < 2; index++) {
            var response = putProfile(Map.of("displayName", "  " + name + "\u2003"), bearer);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("cache-control")).hasValue("no-store");
            var profile = json.readTree(response.body());
            assertProfileFields(profile);
            assertThat(profile.get("displayName").asText()).isEqualTo(name);
        }
        var expected = new HashMap<>(before);
        expected.put("display_name", name);
        assertThat(userRow(account.id)).isEqualTo(expected);
        assertThat(json.readTree(get("/api/users/me", bearer).body()).get("displayName").asText()).isEqualTo(name);
    }

    @Test
    void profileCannotSelectAnotherUserOrMassAssignProtectedFields() throws Exception {
        var first = register();
        var second = register();
        String firstBearer = login(first).get("accessToken").asText();
        String secondBearer = login(second).get("accessToken").asText();
        var firstBefore = userRow(first.id);
        var secondBefore = userRow(second.id);
        var body = Map.of("displayName", "Only first member", "id", second.id.toString(),
                "userId", second.id.toString(), "email", second.email, "username", "changed_name",
                "password", UUID.randomUUID().toString(), "passwordHash", "not-a-hash",
                "avatarPath", "/private/other-user.jpg", "createdAt", "2000-01-01T00:00:00Z");
        var response = send("PUT", "/api/users/me?userId=" + second.id,
                json.writeValueAsString(body), firstBearer, null);
        assertThat(response.statusCode()).isEqualTo(200);
        var profile = json.readTree(response.body());
        assertProfileFields(profile);
        assertThat(profile.get("id").asText()).isEqualTo(first.id.toString());
        var expected = new HashMap<>(firstBefore);
        expected.put("display_name", "Only first member");
        assertThat(userRow(first.id)).isEqualTo(expected);
        assertThat(userRow(second.id)).isEqualTo(secondBefore);
        assertThat(json.readTree(get("/api/users/me?id=" + second.id, firstBearer).body()).get("id").asText())
                .isEqualTo(first.id.toString());
        var secondProfile = json.readTree(get("/api/users/me", secondBearer).body());
        assertThat(secondProfile.get("id").asText()).isEqualTo(second.id.toString());
        assertThat(secondProfile.get("displayName").asText()).isEqualTo("Test member");
        assertThat(get("/api/users/" + second.id, firstBearer).statusCode()).isBetween(400, 499);
        assertThat(send("PUT", "/api/users/" + second.id, json.writeValueAsString(body), firstBearer, null)
                .statusCode()).isBetween(400, 499);
        assertThat(userRow(second.id)).isEqualTo(secondBefore);
    }

    @Test
    void invalidProfileBodiesNeverChangeStoredData() throws Exception {
        var account = register();
        String bearer = login(account).get("accessToken").asText();
        var before = userRow(account.id);
        for (String body : List.of("{}", "{\"displayName\":null}", "{\"displayName\":\"\"}",
                "{\"displayName\":\" \\t\\n\u2003\"}", "{", "[]", "null",
                "{\"displayName\":{}}", "{\"displayName\":[]}",
                json.writeValueAsString(Map.of("displayName", "x".repeat(256))))) {
            assertError(send("PUT", "/api/users/me", body, bearer, null), 400, "VALIDATION_ERROR");
            assertThat(userRow(account.id)).isEqualTo(before);
        }
        assertError(send("PUT", "/api/users/me", null, bearer, null), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(userRow(account.id)).isEqualTo(before);
    }

    @Test
    void profileNameLengthBoundaryMatchesRegistrationValidation() throws Exception {
        var account = register();
        String bearer = login(account).get("accessToken").asText();
        String name = "📷".repeat(127) + "x";
        assertThat(name.length()).isEqualTo(255);
        assertThat(putProfile(Map.of("displayName", name), bearer).statusCode()).isEqualTo(200);
        assertError(putProfile(Map.of("displayName", name + "x"), bearer), 400, "VALIDATION_ERROR");
        assertThat(userRow(account.id).get("display_name")).isEqualTo(name);
    }

    @Test
    void profileRequiresValidAccessJwtForReadsAndWrites() throws Exception {
        var account = register();
        var pair = login(account);
        var before = userRow(account.id);
        String access = pair.get("accessToken").asText();
        var forgedClaims = JwtClaimsSet.builder().subject(account.id.toString())
                .issuer(TokenConfiguration.ISSUER).audience(List.of(TokenConfiguration.AUDIENCE))
                .issuedAt(clock.instant()).expiresAt(clock.instant().plusSeconds(3600)).build();
        String forged = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(randomBytes(), "HmacSHA256")))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), forgedClaims))
                .getTokenValue();
        clock.advance(Duration.ofMinutes(15));
        for (String bearer : java.util.Arrays.asList(null, "invalid.jwt.token", pair.get("refreshToken").asText(), access, forged)) {
            assertError(get("/api/users/me", bearer), 401, "UNAUTHORIZED");
            assertError(putProfile(Map.of("displayName", "Unauthorized update"), bearer), 401, "UNAUTHORIZED");
        }
        assertThat(userRow(account.id)).isEqualTo(before);
        var rotated = post("/refresh", refreshBody(pair), access);
        assertThat(rotated.statusCode()).isEqualTo(200);
        assertThat(get("/api/users/me", json.readTree(rotated.body()).get("accessToken").asText()).statusCode())
                .isEqualTo(200);
    }

    @Test
    void profileRejectsSignedJwtWhoseUserDoesNotExist() throws Exception {
        var claims = JwtClaimsSet.builder().subject(UUID.randomUUID().toString())
                .issuer(TokenConfiguration.ISSUER).audience(List.of(TokenConfiguration.AUDIENCE))
                .issuedAt(clock.instant()).expiresAt(clock.instant().plusSeconds(900)).build();
        String bearer = context.getBean("jwtEncoder", JwtEncoder.class).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertError(get("/api/users/me", bearer), 401, "UNAUTHORIZED");
        assertError(putProfile(Map.of("displayName", "Unknown user"), bearer), 401, "UNAUTHORIZED");
    }

    @Test
    void curlExercisesAuthLifecycleIncludingExpiryForgeryAndDuplicateUsername() throws Exception {
        String username = "curl_" + UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        String email = username + "@example.test";
        var registration = Map.of("email", email, "username", username, "displayName", "Curl member", "password", password);
        var created = curl("POST", "/api/auth/register", registration, null);
        assertThat(created.status).isEqualTo(201);
        String id = json.readTree(created.body).get("id").asText();
        var duplicate = new HashMap<>(registration);
        duplicate.put("email", "other_" + email);
        assertCurlError(curl("POST", "/api/auth/register", duplicate, null), 409, "ACCOUNT_CONFLICT", "/api/auth/register");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE username = ?", Integer.class, username)).isEqualTo(1);
        assertCurlError(curl("GET", "/api/users/me", null, null), 401, "UNAUTHORIZED", "/api/users/me");
        assertCurlError(curl("POST", "/api/auth/login", Map.of("email", email, "password", UUID.randomUUID().toString()), null),
                401, "UNAUTHORIZED", "/api/auth/login");
        var loggedIn = curl("POST", "/api/auth/login", Map.of("email", email, "password", password), null);
        assertThat(loggedIn.status).isEqualTo(200);
        var pair = json.readTree(loggedIn.body);
        String access = pair.get("accessToken").asText();
        var profile = curl("GET", "/api/users/me", null, access);
        assertThat(profile.status).isEqualTo(200);
        assertProfileFields(json.readTree(profile.body));
        assertThat(json.readTree(profile.body).get("id").asText()).isEqualTo(id);
        var updated = curl("PUT", "/api/users/me", Map.of("displayName", "  Curl 📷 member  "), access);
        assertThat(updated.status).isEqualTo(200);
        assertThat(json.readTree(updated.body).get("displayName").asText()).isEqualTo("Curl 📷 member");
        String[] parts = access.split("\\.");
        // Đổi byte đầu chữ ký, giữ header/claims hợp lệ để kiểm tra forged JWT thực sự.
        String forged = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertCurlError(curl("GET", "/api/users/me", null, forged), 401, "UNAUTHORIZED", "/api/users/me");
        clock.advance(Duration.ofMinutes(15));
        assertCurlError(curl("GET", "/api/users/me", null, access), 401, "UNAUTHORIZED", "/api/users/me");
        var refreshed = curl("POST", "/api/auth/refresh", refreshBody(pair), access);
        assertThat(refreshed.status).isEqualTo(200);
        var rotated = json.readTree(refreshed.body);
        assertCurlError(curl("POST", "/api/auth/refresh", refreshBody(pair), null), 401, "UNAUTHORIZED", "/api/auth/refresh");
        var reloaded = curl("GET", "/api/users/me", null, rotated.get("accessToken").asText());
        assertThat(reloaded.status).isEqualTo(200);
        assertThat(json.readTree(reloaded.body).get("displayName").asText()).isEqualTo("Curl 📷 member");
        var loggedOut = curl("POST", "/api/auth/logout", refreshBody(rotated), null);
        assertThat(loggedOut.status).isEqualTo(204);
        assertThat(loggedOut.body).isEmpty();
        assertCurlError(curl("POST", "/api/auth/refresh", refreshBody(rotated), null), 401, "UNAUTHORIZED", "/api/auth/refresh");
        assertThat(sessionCount(UUID.fromString(id), true)).isZero();
    }

    @Test
    void routingErrorsKeepJsonContractWithoutErrorDispatchMasking() throws Exception {
        var account = register();
        String bearer = login(account).get("accessToken").asText();
        assertError(get("/api/unknown-route?credential=must-not-echo", bearer), 404, "NOT_FOUND");
        var unsupported = send("POST", "/api/users/me", "{}", bearer, null);
        assertError(unsupported, 405, "METHOD_NOT_ALLOWED");
        assertThat(unsupported.headers().firstValue("allow").orElse("")).contains("GET", "PUT");
        assertError(get("/api/auth/register", bearer), 405, "METHOD_NOT_ALLOWED");
        var unacceptable = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/users/me"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + bearer)
                .header("Accept", "text/html").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertError(unacceptable, 406, "NOT_ACCEPTABLE");
        // Không mở route hoặc error dispatcher cho request chưa xác thực.
        assertError(get("/api/unknown-route", null), 401, "UNAUTHORIZED");
        assertError(get("/error", bearer), 403, "FORBIDDEN");
    }

    @Test
    void unexpectedMvcFailureReturnsSafeCommonError() throws Exception {
        String bearer = login(register()).get("accessToken").asText();
        var response = get("/api/test/failure", bearer);
        assertError(response, 500, "INTERNAL_ERROR");
        assertThat(response.body()).doesNotContain("fixture-sensitive-value", "IllegalStateException", "stackTrace");
    }

    private CurlResponse curl(String method, String path, Object body, String bearer) throws Exception {
        String executable = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win") ? "curl.exe" : "curl";
        // Không đưa password/JWT vào command line, file hoặc output test; truyền config bằng stdin.
        StringBuilder config = new StringBuilder("silent\nshow-error\nconnect-timeout = 5\nmax-time = 15\nnoproxy = \"*\"\n");
        config.append("url = ").append(curlQuote(baseUrl + path)).append('\n');
        config.append("request = ").append(curlQuote(method)).append('\n');
        config.append("write-out = \"\\n%{http_code}\"\n");
        if (bearer != null) { config.append("header = ").append(curlQuote("Authorization: Bearer " + bearer)).append('\n'); }
        if (body != null) {
            config.append("header = \"Content-Type: application/json\"\n");
            config.append("data-binary = ").append(curlQuote(json.writeValueAsString(body))).append('\n');
        }
        Process process = new ProcessBuilder(executable, "--disable", "--config", "-").start();
        try {
            try (var input = process.outputWriter(java.nio.charset.StandardCharsets.UTF_8)) { input.write(config.toString()); }
            if (!process.waitFor(20, TimeUnit.SECONDS)) { throw new IllegalStateException("curl request timed out."); }
            // Nội dung lỗi curl không in ra vì có thể chứa thông tin request.
            assertThat(process.exitValue()).as("curl transport exit code").isZero();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int boundary = output.lastIndexOf('\n');
            if (boundary < 0) { throw new IllegalStateException("Missing curl status trailer."); }
            return new CurlResponse(Integer.parseInt(output.substring(boundary + 1).strip()), output.substring(0, boundary));
        } finally { if (process.isAlive()) { process.destroyForcibly(); } }
    }

    private static String curlQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    private void assertCurlError(CurlResponse response, int status, String code, String path) throws Exception {
        assertThat(response.status).isEqualTo(status);
        var value = json.readTree(response.body);
        assertThat(value.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("timestamp", "status", "code", "message", "path");
        assertThat(value.get("status").asInt()).isEqualTo(status);
        assertThat(value.get("code").asText()).isEqualTo(code);
        assertThat(value.get("path").asText()).isEqualTo(path);
        assertThat(Instant.parse(value.get("timestamp").asText())).isEqualTo(clock.instant());
        assertThat(value.get("message").asText()).isIn("Thông tin xác thực không hợp lệ hoặc đã hết hạn.",
                "Email hoặc username đã được sử dụng.");
    }

    private record CurlResponse(int status, String body) {
        @Override public String toString() { return "CurlResponse[status=" + status + ", body=redacted]"; }
    }

    private Map<String, Object> userRow(UUID id) {
        return jdbc.queryForMap("SELECT id, email, username, password_hash, display_name, avatar_path, created_at"
                + " FROM users WHERE id = ?", id);
    }

    private void assertProfileFields(JsonNode profile) {
        assertThat(profile.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("id", "email", "username", "displayName", "createdAt");
    }

    private HttpResponse<String> putProfile(Object body, String bearer) throws Exception {
        return send("PUT", "/api/users/me", json.writeValueAsString(body), bearer, null);
    }

    private Account register() throws Exception {
        String username = "auth_" + UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        String email = username + "@example.test";
        var response = post("/register", Map.of("email", email, "username", username, "displayName", "Test member",
                "password", password), null);
        assertThat(response.statusCode()).isEqualTo(201);
        return new Account(UUID.fromString(json.readTree(response.body()).get("id").asText()), email, password);
    }

    private JsonNode login(Account account) throws Exception {
        var response = post("/login", Map.of("email", account.email, "password", account.password), null);
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body());
    }

    private Map<String, String> refreshBody(JsonNode pair) { return Map.of("refreshToken", pair.get("refreshToken").asText()); }
    private HttpResponse<String> post(String path, Object body, String bearer) throws Exception {
        return send("POST", "/api/auth" + path, json.writeValueAsString(body), bearer, null);
    }
    private HttpResponse<String> probe(String bearer) throws Exception { return get("/api/test/protected", bearer); }
    private HttpResponse<String> get(String path, String bearer) throws Exception { return send("GET", path, null, bearer, null); }

    private HttpResponse<String> send(String method, String path, String body, String bearer, String forwarded) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20));
        if (bearer != null) { builder.header("Authorization", "Bearer " + bearer); }
        if (forwarded != null) { builder.header("X-Forwarded-For", forwarded); }
        if (body != null) { builder.header("Content-Type", "application/json"); }
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertError(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        assertThat(response.headers().firstValue("cache-control").orElse("")).contains("no-store");
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
        var value = json.readTree(response.body());
        assertThat(value.propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("timestamp", "status", "code", "message", "path");
        assertThat(value.get("code").asText()).isEqualTo(code);
        assertThat(value.get("status").asInt()).isEqualTo(status);
        assertThat(value.get("path").asText()).isEqualTo(response.uri().getPath());
        assertThat(Instant.parse(value.get("timestamp").asText())).isEqualTo(clock.instant());
        assertThat(value.get("message").asText()).isIn("Dữ liệu yêu cầu không hợp lệ.",
                "Thông tin xác thực không hợp lệ hoặc đã hết hạn.", "Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau.",
                "Bạn không có quyền thực hiện yêu cầu này.", "Không thể xử lý yêu cầu.", "Yêu cầu JSON.",
                "Không tìm thấy tài nguyên.", "Phương thức không được hỗ trợ.", "Response chỉ hỗ trợ JSON.");
    }

    private int sessionCount(UUID user, boolean active) {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ?"
                + (active ? " AND revoked_at IS NULL" : ""), Integer.class, user);
    }

    private static byte[] randomBytes() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return bytes; }
    private static String opaqueToken() { return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes()); }
    private record Account(UUID id, String email, String password) {
        @Override public String toString() { return "Account[redacted]"; }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean ProbeController probeController() { return new ProbeController(); }
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
        @Bean @Primary ControlledEncoder controlledEncoder(@Qualifier("jwtEncoder") JwtEncoder encoder) {
            return new ControlledEncoder(encoder);
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        void advance(Duration duration) { now.updateAndGet(instant -> instant.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return now.get(); }
    }

    static class ControlledEncoder implements JwtEncoder {
        private final JwtEncoder delegate;
        private final AtomicBoolean failNext = new AtomicBoolean();
        ControlledEncoder(JwtEncoder delegate) { this.delegate = delegate; }
        @Override public Jwt encode(JwtEncoderParameters parameters) {
            if (failNext.getAndSet(false)) { throw new JwtEncodingException("Simulated test failure."); }
            return delegate.encode(parameters);
        }
    }

    // Controller chỉ có trong test source/profile, không nằm trong production JAR hoặc /users/me (2C).
    @RestController
    @Profile("auth-test")
    static class ProbeController {
        @GetMapping("/api/test/failure")
        public void failure() { throw new IllegalStateException("fixture-sensitive-value"); }

        @GetMapping("/api/test/protected")
        public Map<String, String> subject(@AuthenticationPrincipal Jwt jwt) { return Map.of("subject", jwt.getSubject()); }
    }
}
