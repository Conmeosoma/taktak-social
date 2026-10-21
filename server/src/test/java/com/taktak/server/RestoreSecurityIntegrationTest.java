package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

/** SQL deploy that runs against real PostgreSQL; HTTP uses the actual JWT/refresh filters. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestoreSecurityIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private PasswordEncoder passwords;
    private String base, reconciliation;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    @BeforeAll void start() throws Exception {
        Path root=Files.createDirectories(Path.of("target","restore-security"));
        postgres=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setPort(0)
                .setServerConfig("listen_addresses","127.0.0.1").start();
        byte[] key=new byte[32]; new SecureRandom().nextBytes(key);
        context=(ServletWebServerApplicationContext)new SpringApplication(TaktakApplication.class).run(
                "--spring.datasource.url="+postgres.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres","--spring.datasource.password=",
                "--server.address=127.0.0.1","--server.port=0","--logging.level.root=WARN",
                "--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key));
        jdbc=context.getBean(JdbcTemplate.class); passwords=context.getBean(PasswordEncoder.class);
        base="http://127.0.0.1:"+context.getWebServer().getPort();
        reconciliation=Files.readString(Path.of("..","deploy","scripts","reconcile-restore.sql"));
    }
    @AfterAll void close() throws Exception {
        try { if(context!=null)context.close(); } finally { if(postgres!=null)postgres.close(); }
    }
    @BeforeEach void isolateFixture() { jdbc.execute("TRUNCATE TABLE users CASCADE"); }
    private UUID account(String password) {
        UUID id=UUID.randomUUID(); String name="restore_"+id.toString().replace("-","");
        jdbc.update("insert into users(id,email,username,password_hash,display_name,security_version,email_verified_at) values(?,?,?,?,?,7,now())",
                id,id+"@example.test",name,passwords.encode(password),"Restore QA");
        return id;
    }
    private HttpResponse<String> post(String path,Map<String,String> body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/auth"+path))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode login(UUID id,String password) throws Exception {
        var result=post("/login",Map.of("email",id+"@example.test","password",password));
        assertThat(result.statusCode()).isEqualTo(200); return json.readTree(result.body());
    }
    private int me(JsonNode pair) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/users/me"))
                .header("Authorization","Bearer "+pair.get("accessToken").asText()).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    private Connection snapshotSecurity() throws Exception {
        var connection=Objects.requireNonNull(jdbc.getDataSource()).getConnection(); connection.setAutoCommit(false);
        try(var statement=connection.createStatement()) {
            // Hikari tra connection ve pool, temp table van o physical session.
            statement.execute("DROP TABLE IF EXISTS pg_temp.restore_security_before");
            statement.execute("CREATE TEMP TABLE restore_security_before AS SELECT id,security_version,password_hash,email,username,email_verified_at FROM users");
        }
        connection.commit(); return connection;
    }
    @Test void restoredOldAndCurrentJwtRefreshAndRecoveryCannotEscapeRevocation() throws Exception {
        String original=UUID.randomUUID().toString(), replacement=UUID.randomUUID().toString();
        UUID id=account(original); String oldHash=jdbc.queryForObject("select password_hash from users where id=?",String.class,id);
        var old=login(id,original);
        // Mo phong reset da hoan tat sau backup; recovery API duoc test rieng.
        jdbc.update("update users set security_version=9,password_hash=? where id=?",passwords.encode(replacement),id);
        jdbc.update("update refresh_tokens set revoked_at=now() where user_id=?",id);
        var current=login(id,replacement); assertThat(me(old)).isEqualTo(401); assertThat(me(current)).isEqualTo(200);
        try(var connection=snapshotSecurity()) {
            jdbc.update("update users set security_version=7,password_hash=?,email_verified_at=null where id=?",oldHash,id);
            jdbc.update("update refresh_tokens set revoked_at=null where user_id=?",id);
            jdbc.update("insert into account_recovery(user_id,reset_hash,reset_expires_at) values(?,'QA stale recovery',now()+interval '10 minutes')",id);
            assertThat(me(old)).as("Red baseline: unreconciled snapshot revives the old JWT").isEqualTo(200);
            try(var statement=connection.createStatement()) { statement.execute(reconciliation); }
            connection.commit();
        }
        assertThat(jdbc.queryForObject("select security_version from users where id=?",Long.class,id)).isEqualTo(10);
        for(var pair:List.of(old,current)) {
            assertThat(me(pair)).isEqualTo(401);
            assertThat(post("/refresh",Map.of("refreshToken",pair.get("refreshToken").asText())).statusCode()).isEqualTo(401);
        }
        assertThat(jdbc.queryForObject("select count(*) from account_recovery where user_id=?",Integer.class,id)).isZero();
        assertThat(jdbc.queryForObject("select email_verified_at is not null from users where id=?",Boolean.class,id)).isTrue();
        assertThat(post("/login",Map.of("email",id+"@example.test","password",original)).statusCode()).isEqualTo(401);
        assertThat(me(login(id,replacement))).isEqualTo(200);
    }
    @Test void unknownRestoredAccountFailsAtomicallyInsteadOfBeingResurrected() throws Exception {
        UUID known=account(UUID.randomUUID().toString());
        try(var connection=snapshotSecurity()) {
            account(UUID.randomUUID().toString()); // account chi co trong snapshot duoc restore, khong trong security-before
            assertThatThrownBy(() -> { try(var statement=connection.createStatement()) { statement.execute(reconciliation); } })
                    .isInstanceOf(java.sql.SQLException.class);
            connection.rollback();
        }
        assertThat(jdbc.queryForObject("select security_version from users where id=?",Long.class,known)).isEqualTo(7);
    }
    @Test void versionOverflowFailsAtomicallyWithoutResettingOtherUsers() throws Exception {
        UUID id=account(UUID.randomUUID().toString()); jdbc.update("update users set security_version=? where id=?",Long.MAX_VALUE,id);
        try(var connection=snapshotSecurity()) {
            assertThatThrownBy(() -> { try(var statement=connection.createStatement()) { statement.execute(reconciliation); } })
                    .isInstanceOf(java.sql.SQLException.class);
            connection.rollback();
        }
        assertThat(jdbc.queryForObject("select security_version from users where id=?",Long.class,id)).isEqualTo(Long.MAX_VALUE);
        // Khong de fixture overflow anh huong ca khac.
        jdbc.update("update users set security_version=7 where id=?",id);
    }
    @Test void newerSnapshotVersionIsAlsoExceeded() throws Exception {
        UUID id=account(UUID.randomUUID().toString());
        try(var connection=snapshotSecurity()) {
            jdbc.update("update users set security_version=12 where id=?",id);
            try(var statement=connection.createStatement()) { statement.execute(reconciliation); }
            connection.commit();
        }
        assertThat(jdbc.queryForObject("select security_version from users where id=?",Long.class,id)).isEqualTo(13);
    }
}
