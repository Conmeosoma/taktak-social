package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.AccountMail;
import com.taktak.server.auth.SmtpAccountMail;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: -Dtest=GmailRecoveryAcceptanceIT. Gửi thật3thư; không chạy trong suite mặc định. */
class GmailRecoveryAcceptanceIT {
    private final ObjectMapper json=new ObjectMapper();
    private final HttpClient http=HttpClient.newHttpClient();
    private String base;

    @Test void smtpConnectionOnly() throws Exception {
        var adapter=new SmtpAccountMail(System.getenv("MAIL_HOST"),Integer.parseInt(System.getenv("MAIL_PORT")),
                System.getenv("MAIL_USERNAME"),System.getenv("MAIL_PASSWORD"),System.getenv("MAIL_FROM"));
        var field=SmtpAccountMail.class.getDeclaredField("sender"); field.setAccessible(true);
        try {
            ((org.springframework.mail.javamail.JavaMailSenderImpl)field.get(adapter)).testConnection();
            System.out.println("SMTP_CONNECTION_TLS_AUTH=PASS; EMAILS_SENT=0");
        } catch(Exception failure) {
            safeCategory(failure);
            throw new AssertionError("SMTP connection/TLS/authentication failed; no email sent.");
        }
    }

    @Test void realSmtpAcceptsVerificationTemporaryAndChangedNotification() throws Exception {
        // Chỉ biến môi trường; không file credential/argv/request logging hoặc giá trị bí mật trong assertion.
        String email=System.getenv("TAKTAK_QA_EMAIL");
        assertThat(email!=null && email.contains("@")).as("SMTP environment configured").isTrue();
        Path root=Files.createDirectories(Path.of("target","gmail-acceptance"));
        try(var postgres=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setPort(0)
                .setServerConfig("listen_addresses","127.0.0.1").start()) {
            var application=new SpringApplication(TaktakApplication.class,MailBeans.class);
            try(var context=(ServletWebServerApplicationContext)application.run(
                    "--spring.datasource.url="+postgres.getJdbcUrl("postgres","postgres"),
                    "--spring.datasource.username=postgres","--spring.datasource.password=","--server.address=127.0.0.1","--server.port=0",
                    "--debug=false","--logging.level.root=WARN","--logging.level.org.springframework=WARN",
                    "--logging.level.org.hibernate=WARN","--taktak.jwt.access-secret="+signingKey())) {
                base="http://127.0.0.1:"+context.getWebServer().getPort();
                var mail=context.getBean(ForwardingMail.class);
                String original=secret(), replacement=secret();
                var registered=post("/register",Map.of("email",email,"password",original,
                        "username","gmail_qa_"+UUID.randomUUID().toString().replace("-",""),"displayName","Recovery QA"));
                status(registered,201);
                var old=body(post("/login",Map.of("email",email,"password",original)),200);
                status(post("/verification/request",Map.of("email",email,"password",original)),200);
                String code=mail.code(0);
                status(post("/verification/confirm",Map.of("email",email,"password",original,"code",code)),200);
                status(post("/forgot-password",Map.of("email",email)),200);
                String temporary=mail.code(1);
                // API/job giữ user-lock đến commit; login đợi commit thay vì đọc hash chưa lưu.
                JsonNode restricted=body(post("/login",Map.of("email",email,"password",temporary)),200);
                assertThat(restricted.path("resetRequired").asBoolean()).isTrue();
                assertThat(restricted.has("accessToken") || restricted.has("refreshToken")).isFalse();
                String token=restricted.path("resetToken").asText();
                status(get("/api/users/me",token),401);
                status(post("/reset-password",Map.of("resetToken",token,"temporaryPassword",temporary,
                        "newPassword",replacement,"confirmation",replacement)),200);
                mail.await(2);
                status(get("/api/users/me",old.path("accessToken").asText()),401);
                status(post("/refresh",Map.of("refreshToken",old.path("refreshToken").asText())),401);
                status(post("/login",Map.of("email",email,"password",original)),401);
                status(post("/login",Map.of("email",email,"password",temporary)),401);
                JsonNode fresh=body(post("/login",Map.of("email",email,"password",replacement)),200);
                status(get("/api/users/me",fresh.path("accessToken").asText()),200);
                assertThat(mail.messages.size()).isEqualTo(3);
                assertThat(mail.messages.get(2).contains(original) || mail.messages.get(2).contains(temporary)
                        || mail.messages.get(2).contains(replacement)).as("Notification excludes credentials").isFalse();
                assertThat(context.getBean(JdbcTemplate.class).queryForObject(
                        "select count(*) from users where email_verified_at is not null and security_version=1",Integer.class)).isEqualTo(1);
                System.out.println("GMAIL_QA_SMTP_ACCEPTED=3; API_RESET_AND_REVOCATION=PASS; INBOX_CONFIRMATION=REQUIRED");
                // Thư thực sự đi qua Gmail SMTP; code lấy trong memory của sender fixture, không đọc mailbox/IMAP.
                // Chỉ người dùng xác nhận inbox mới được ghi delivery PASS.
            }
        }
    }
    private HttpResponse<String> post(String path,Object value) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/auth"+path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path,String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20))
                .header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private void status(HttpResponse<String> response,int status) { assertThat(response.statusCode()).isEqualTo(status); }
    private JsonNode body(HttpResponse<String> response,int status) throws Exception { status(response,status); return json.readTree(response.body()); }
    private static String secret() { byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String signingKey() { byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getEncoder().encodeToString(bytes); }
    @TestConfiguration(proxyBeanMethods=false) static class MailBeans {
        @Bean @Primary ForwardingMail forwardingMail(SmtpAccountMail delegate) { return new ForwardingMail(delegate); }
    }
    static final class ForwardingMail implements AccountMail {
        private final SmtpAccountMail delegate;
        private final List<String> messages=new CopyOnWriteArrayList<>();
        private final String marker="[Taktak QA 19-10 "+UUID.randomUUID().toString().substring(0,8)+"] ";
        ForwardingMail(SmtpAccountMail delegate) { this.delegate=delegate; }
        @Override public void send(String recipient,String subject,String text) {
            try { delegate.send(recipient,marker+subject,text+"\n\nThư kiểm thử tài khoản QA local riêng, không thay đổi tài khoản Taktak cá nhân."); }
            catch(RuntimeException failure) { safeCategory(failure); throw failure; }
            messages.add(text);
        }
        void await(int index) throws InterruptedException {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
            while(messages.size()<=index && System.nanoTime()<deadline) Thread.sleep(50);
            assertThat(messages.size()>index).as("Real SMTP acceptance observed").isTrue();
        }
        String code(int index) throws InterruptedException {
            await(index); var matcher=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}").matcher(messages.get(index));
            assertThat(matcher.find()).as("Credential present in outgoing memory fixture only").isTrue(); return matcher.group();
        }
    }
    private static void safeCategory(Throwable failure) {
        var types=new ArrayList<String>();
        for(Throwable cause=failure;cause!=null && types.size()<8;cause=cause.getCause()) types.add(cause.getClass().getSimpleName());
        System.out.println("SMTP_FAILURE_TYPES="+String.join("/",types));
    }
}
