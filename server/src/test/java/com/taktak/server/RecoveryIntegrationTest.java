package com.taktak.server;

import com.fasterxml.jackson.databind.*;
import com.taktak.server.auth.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecoveryIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private TestClock clock;
    private MailFixture mail;
    private String base;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    @BeforeAll void start() throws Exception {
        Path root = Files.createDirectories(Path.of("target", "recovery-tests"));
        postgres = EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setPort(0)
                .setServerConfig("listen_addresses", "127.0.0.1").start();
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        var app = new SpringApplication(TaktakApplication.class, Beans.class);
        context = (ServletWebServerApplicationContext) app.run("--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=", "--server.port=0",
                "--server.address=127.0.0.1", "--logging.level.root=WARN",
                "--taktak.jwt.access-secret=" + Base64.getEncoder().encodeToString(key));
        jdbc = context.getBean(JdbcTemplate.class); clock = context.getBean(TestClock.class);
        mail = context.getBean(MailFixture.class); base = "http://127.0.0.1:" + context.getWebServer().getPort();
    }
    @AfterAll void close() throws Exception {
        try { if (context != null) context.close(); } finally { if (postgres != null) postgres.close(); }
    }
    @BeforeEach void advance() { clock.advance(120); mail.fail = false; }

    @Test void verificationNeedsBothOriginalPasswordAndMailboxCodeAndExpires() throws Exception {
        var a = register();
        assertThat(post("/verification/request", Map.of("email", a.email, "password", UUID.randomUUID().toString())).statusCode()).isEqualTo(401);
        assertThat(mail.messages.containsKey(a.email)).isFalse();
        assertThat(post("/verification/request", loginBody(a)).statusCode()).isEqualTo(200);
        String code = mail.code(a.email);
        assertThat(jdbc.queryForObject("select verification_hash from account_recovery where user_id=?", String.class, a.id))
                .isEqualTo(TokenService.hash(code)).isNotEqualTo(code);
        assertThat(post("/verification/confirm", Map.of("email", a.email, "password", a.password, "code", secret())).statusCode()).isEqualTo(401);
        assertThat(post("/verification/confirm", Map.of("email", a.email, "password", a.password, "code", code)).statusCode()).isEqualTo(200);
        clock.advance(61);
        assertThat(post("/verification/confirm", Map.of("email", a.email, "password", a.password, "code", code)).statusCode()).isEqualTo(401);
        var b = register(); post("/verification/request", loginBody(b)); String expired = mail.code(b.email); clock.advance(900);
        assertThat(post("/verification/confirm", Map.of("email", b.email, "password", b.password, "code", expired)).statusCode()).isEqualTo(401);
    }
    @Test void forgotResponseIsGenericAndUnverifiedAccountsCannotRecover() throws Exception {
        var a = register(); String before = hash(a);
        var known = post("/forgot-password", Map.of("email", a.email));
        var unknown = post("/forgot-password", Map.of("email", secret() + "@example.test"));
        assertThat(known.statusCode()).isEqualTo(200); assertThat(unknown.body()).isEqualTo(known.body());
        assertThat(json.readTree(known.body()).get("message").asText()).isEqualTo(RecoveryService.GENERIC);
        assertThat(post("/forgot-password", Map.of("email", "bad")).statusCode()).isEqualTo(400);
        assertThat(hash(a)).isEqualTo(before);
        assertThat(post("/login", loginBody(a)).statusCode()).isEqualTo(200);
        assertThat(mail.messages.containsKey(a.email)).isFalse();
    }
    @Test void resetRevokesEveryOldJwtAndRefreshAndRequiresFreshLogin() throws Exception {
        var a = register(); verify(a); clock.advance(61);
        JsonNode old = tree(post("/login", loginBody(a))), second = tree(post("/login", loginBody(a)));
        String temporary = temporary(a);
        // Request không đổi mật khẩu thật hoặc thu hồi phiên trước khi người dùng hoàn tất.
        assertThat(get("/api/users/me", old.get("accessToken").asText()).statusCode()).isEqualTo(200);
        var limited = tree(post("/login", Map.of("email", a.email, "password", temporary)));
        assertThat(limited.has("accessToken")).isFalse(); assertThat(limited.has("refreshToken")).isFalse();
        String reset = limited.get("resetToken").asText();
        for (String path : List.of("/api/users/me", "/api/friends", "/api/moments?type=sent")) {
            assertThat(get(path, reset).statusCode()).isEqualTo(401);
        }
        assertThat(post("/refresh", Map.of("refreshToken", reset)).statusCode()).isEqualTo(401);
        assertThat(post("/login", Map.of("email", a.email, "password", temporary)).statusCode()).isEqualTo(401);
        clock.advance(61);
        String replacement = secret(); var body = resetBody(reset, temporary, replacement);
        var mismatch = new HashMap<>(body); mismatch.put("confirmation", secret());
        assertThat(post("/reset-password", mismatch).statusCode()).isEqualTo(400);
        assertThat(post("/reset-password", resetBody(reset, temporary, a.password)).statusCode()).isEqualTo(400);
        assertThat(post("/reset-password", body).statusCode()).isEqualTo(200);
        assertThat(post("/reset-password", body).statusCode()).isEqualTo(401);
        assertThat(post("/login", loginBody(a)).statusCode()).isEqualTo(401);
        assertThat(post("/login", Map.of("email", a.email, "password", temporary)).statusCode()).isEqualTo(401);
        clock.advance(61);
        for (JsonNode pair : List.of(old, second)) {
            assertThat(get("/api/users/me", pair.get("accessToken").asText()).statusCode()).isEqualTo(401);
            assertThat(post("/refresh", Map.of("refreshToken", pair.get("refreshToken").asText())).statusCode()).isEqualTo(401);
        }
        var fresh = tree(post("/login", Map.of("email", a.email, "password", replacement)));
        assertThat(get("/api/users/me", fresh.get("accessToken").asText()).statusCode()).isEqualTo(200);
        assertThat(mail.messages.get(a.email)).doesNotContain(replacement, temporary);
    }
    @Test void temporaryPasswordWrongExpiredOrResetExpiredCannotChangeAccount() throws Exception {
        var a = register(); verify(a); clock.advance(61); String temporary = temporary(a);
        assertThat(post("/login", Map.of("email", a.email, "password", secret())).statusCode()).isEqualTo(401);
        clock.advance(900);
        assertThat(post("/login", Map.of("email", a.email, "password", temporary)).statusCode()).isEqualTo(401);
        temporary = temporary(a);
        String token = tree(post("/login", Map.of("email", a.email, "password", temporary))).get("resetToken").asText();
        clock.advance(900);
        assertThat(post("/reset-password", resetBody(token, temporary, secret())).statusCode()).isEqualTo(401);
        assertThat(post("/login", loginBody(a)).statusCode()).isEqualTo(200);
    }
    @Test void concurrentResetConsumesSessionExactlyOnce() throws Exception {
        var a = register(); verify(a); clock.advance(61); String temporary = temporary(a);
        String token = tree(post("/login", Map.of("email", a.email, "password", temporary))).get("resetToken").asText();
        var pool = Executors.newFixedThreadPool(2); var barrier = new CyclicBarrier(2);
        try {
            var body = resetBody(token, temporary, secret());
            Callable<Integer> call = () -> { barrier.await(5, TimeUnit.SECONDS); return post("/reset-password", body).statusCode(); };
            var first = pool.submit(call); var second = pool.submit(call);
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 401);
            assertThat(jdbc.queryForObject("select security_version from users where id=?", Long.class, a.id)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void smtpFailureRollsBackTemporaryCredentialAndDoesNotLockOriginalAccount() throws Exception {
        var a = register(); verify(a); clock.advance(61); String original = hash(a); mail.fail = true;
        assertThat(post("/forgot-password", Map.of("email", a.email)).statusCode()).isEqualTo(200);
        // Chờ hàng đợi bằng marker cùng executor không có; gọi service trực tiếp để xác minh rollback SMTP failure.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> context.getBean(RecoveryService.class).issueTemporary(a.email))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
        assertThat(hash(a)).isEqualTo(original);
        assertThat(jdbc.queryForObject("select temporary_hash from account_recovery where user_id=?", String.class, a.id)).isNull();
        assertThat(post("/login", loginBody(a)).statusCode()).isEqualTo(200);
    }
    @Test void rateLimitsAccountAndIpEvenForUnknownEmail() throws Exception {
        String unknown = secret() + "@example.test";
        for (int i = 0; i < 5; i++) assertThat(post("/forgot-password", Map.of("email", unknown)).statusCode()).isEqualTo(200);
        assertThat(post("/forgot-password", Map.of("email", unknown)).statusCode()).isEqualTo(429);
        clock.advance(61);
        for (int i = 0; i < 20; i++) assertThat(post("/forgot-password", Map.of("email", secret()+"@example.test")).statusCode()).isEqualTo(200);
        assertThat(post("/forgot-password", Map.of("email", secret()+"@example.test")).statusCode()).isEqualTo(429);
    }
    @Test void concurrentTemporaryLoginIssuesOnlyOneRestrictedSession() throws Exception {
        var a=register(); verify(a); clock.advance(61); String temporary=temporary(a);
        var pool=Executors.newFixedThreadPool(2); var barrier=new CyclicBarrier(2);
        try {
            Callable<Integer> call=() -> { barrier.await(5,TimeUnit.SECONDS); return post("/login",Map.of("email",a.email,"password",temporary)).statusCode(); };
            var first=pool.submit(call); var second=pool.submit(call);
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,401);
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id=?",Integer.class,a.id)).isZero();
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentRefreshCannotEscapeResetRevocation() throws Exception {
        var a=register(); verify(a); clock.advance(61);
        var old=tree(post("/login",loginBody(a))); String temporary=temporary(a);
        String token=tree(post("/login",Map.of("email",a.email,"password",temporary))).get("resetToken").asText();
        var pool=Executors.newFixedThreadPool(2); var barrier=new CyclicBarrier(2);
        try {
            var refreshing=pool.submit(() -> { barrier.await(5,TimeUnit.SECONDS); return post("/refresh",Map.of("refreshToken",old.get("refreshToken").asText())); });
            var resetting=pool.submit(() -> { barrier.await(5,TimeUnit.SECONDS); return post("/reset-password",resetBody(token,temporary,secret())); });
            assertThat(resetting.get(20,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            var response=refreshing.get(20,TimeUnit.SECONDS); assertThat(response.statusCode()).isIn(200,401);
            if(response.statusCode()==200) assertThat(get("/api/users/me",tree(response).get("accessToken").asText()).statusCode()).isEqualTo(401);
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id=? and revoked_at is null",Integer.class,a.id)).isZero();
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentOldPasswordLoginCannotEscapeResetRevocation() throws Exception {
        var a=register(); verify(a); clock.advance(61); String temporary=temporary(a);
        String token=tree(post("/login",Map.of("email",a.email,"password",temporary))).get("resetToken").asText();
        var pool=Executors.newFixedThreadPool(2); var barrier=new CyclicBarrier(2);
        try {
            var loggingIn=pool.submit(() -> { barrier.await(5,TimeUnit.SECONDS); return post("/login",loginBody(a)); });
            var resetting=pool.submit(() -> { barrier.await(5,TimeUnit.SECONDS); return post("/reset-password",resetBody(token,temporary,secret())); });
            assertThat(resetting.get(20,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            var response=loggingIn.get(20,TimeUnit.SECONDS); assertThat(response.statusCode()).isIn(200,401);
            if(response.statusCode()==200) assertThat(get("/api/users/me",tree(response).get("accessToken").asText()).statusCode()).isEqualTo(401);
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id=? and revoked_at is null",Integer.class,a.id)).isZero();
        } finally { pool.shutdownNow(); }
    }
    @Test void passwordPolicyAndWrongTemporaryCredentialCannotMutateAccount() throws Exception {
        var a=register(); verify(a); clock.advance(61); String temporary=temporary(a);
        String token=tree(post("/login",Map.of("email",a.email,"password",temporary))).get("resetToken").asText();
        String original=hash(a);
        for(String replacement : List.of("short","📷".repeat(19),"a\0bcdefgh")) {
            assertThat(post("/reset-password",resetBody(token,temporary,replacement)).statusCode()).isEqualTo(400);
        }
        clock.advance(61);
        assertThat(post("/reset-password",resetBody(token,secret(),secret())).statusCode()).isEqualTo(401);
        assertThat(post("/reset-password",resetBody(token,temporary,temporary)).statusCode()).isEqualTo(400);
        assertThat(hash(a)).isEqualTo(original);
    }
    @Test void notificationFailureDoesNotUndoCommittedReset() throws Exception {
        var a=register(); verify(a); clock.advance(61); String temporary=temporary(a);
        String token=tree(post("/login",Map.of("email",a.email,"password",temporary))).get("resetToken").asText();
        String replacement=secret(); mail.fail=true;
        assertThat(post("/reset-password",resetBody(token,temporary,replacement)).statusCode()).isEqualTo(200);
        mail.fail=false;
        assertThat(post("/login",Map.of("email",a.email,"password",replacement)).statusCode()).isEqualTo(200);
    }
    @Test void legacyJwtWithoutVersionIsValidOnlyUntilFirstReset() throws Exception {
        var a=register(); verify(a); clock.advance(61);
        var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().subject(a.id.toString())
                .issuer(com.taktak.server.config.TokenConfiguration.ISSUER)
                .audience(List.of(com.taktak.server.config.TokenConfiguration.AUDIENCE)).issuedAt(clock.instant())
                .expiresAt(clock.instant().plusSeconds(900)).build();
        String legacy=context.getBean(org.springframework.security.oauth2.jwt.JwtEncoder.class).encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
        assertThat(get("/api/users/me",legacy).statusCode()).isEqualTo(200);
        String temporary=temporary(a);
        String token=tree(post("/login",Map.of("email",a.email,"password",temporary))).get("resetToken").asText();
        assertThat(post("/reset-password",resetBody(token,temporary,secret())).statusCode()).isEqualTo(200);
        assertThat(get("/api/users/me",legacy).statusCode()).isEqualTo(401);
    }
    private void verify(Account a) throws Exception {
        assertThat(post("/verification/request", loginBody(a)).statusCode()).isEqualTo(200);
        assertThat(post("/verification/confirm", Map.of("email", a.email, "password", a.password, "code", mail.code(a.email))).statusCode()).isEqualTo(200);
    }
    private String temporary(Account a) throws Exception {
        mail.messages.remove(a.email);
        assertThat(post("/forgot-password", Map.of("email", a.email)).statusCode()).isEqualTo(200);
        long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while (!mail.messages.containsKey(a.email) && System.nanoTime()<deadline) Thread.sleep(25);
        return mail.code(a.email);
    }
    private String hash(Account a) { return jdbc.queryForObject("select password_hash from users where id=?", String.class, a.id); }
    private Map<String,String> resetBody(String token, String temporary, String replacement) {
        return Map.of("resetToken",token,"temporaryPassword",temporary,"newPassword",replacement,"confirmation",replacement);
    }
    private Account register() throws Exception {
        String username = "recovery_"+UUID.randomUUID().toString().replace("-", ""); String password = secret();
        String email = username+"@example.test";
        var response = post("/register", Map.of("email",email,"username",username,"displayName","QA","password",password));
        assertThat(response.statusCode()).isEqualTo(201);
        return new Account(UUID.fromString(tree(response).get("id").asText()),email,password);
    }
    private Map<String,String> loginBody(Account a) { return Map.of("email",a.email,"password",a.password); }
    private JsonNode tree(HttpResponse<String> r) throws Exception { assertThat(r.statusCode()).isEqualTo(200 + (r.uri().getPath().endsWith("register") ? 1 : 0)); return json.readTree(r.body()); }
    private HttpResponse<String> post(String path, Object body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/auth"+path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20))
                .header("Authorization","Bearer "+token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String secret() { byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private record Account(UUID id, String email, String password) { @Override public String toString() { return "Account[redacted]"; } }
    @TestConfiguration(proxyBeanMethods=false) static class Beans {
        @Bean @Primary TestClock recoveryClock() { return new TestClock(); }
        @Bean @Primary MailFixture accountMail() { return new MailFixture(); }
    }
    static class TestClock extends Clock {
        private volatile Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        void advance(long seconds) { now=now.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now,zone); }
        @Override public Instant instant() { return now; }
    }
    static class MailFixture implements AccountMail {
        final Map<String,String> messages=new ConcurrentHashMap<>(); volatile boolean fail;
        @Override public void send(String recipient, String subject, String text) {
            if (fail) throw new org.springframework.mail.MailSendException("Simulated delivery failure.");
            messages.put(recipient,text);
        }
        String code(String email) {
            String text=messages.get(email); assertThat(text).isNotNull();
            var matcher=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}").matcher(text);
            assertThat(matcher.find()).isTrue(); return matcher.group();
        }
    }
}
