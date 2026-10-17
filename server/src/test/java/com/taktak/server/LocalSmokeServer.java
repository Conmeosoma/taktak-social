package com.taktak.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

/** Chạy app thật với cluster PostgreSQL test mới; không có trong JAR production. */
public final class LocalSmokeServer {
    private LocalSmokeServer() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createDirectories(Path.of("target", "smoke-postgres"));
        Path images = Files.createTempDirectory(root, "private-images-");
        int port = Integer.parseInt(System.getProperty("taktak.smoke.port", "0"));
        byte[] testKey = new byte[32];
        new java.security.SecureRandom().nextBytes(testKey);
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setDataDirectory(Files.createTempDirectory(root, "cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses", "127.0.0.1")
                .setPort(0).start();
             ServletWebServerApplicationContext context =
                     (ServletWebServerApplicationContext) SpringApplication.run(TaktakApplication.class,
                             "--taktak.jwt.access-secret=" + java.util.Base64.getEncoder().encodeToString(testKey),
                             "--taktak.jwt.access-minutes=15", "--taktak.jwt.refresh-days=30",
                             "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                             "--spring.datasource.username=postgres",
                             "--spring.datasource.password=",
                             "--taktak.storage.root=" + images.toAbsolutePath(),
                             "--server.address=127.0.0.1",
                             "--server.port=" + port,
                             "--debug=false",
                             "--logging.level.root=WARN")) {
            System.out.println("SMOKE_HEALTH=http://127.0.0.1:" + context.getWebServer().getPort() + "/actuator/health");
            System.out.println("Temporary test database only. Enter stops; offline/online toggles only this test HTTP connector.");
            var console = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            String command;
            var connector = ((org.springframework.boot.web.embedded.tomcat.TomcatWebServer) context.getWebServer()).getTomcat().getConnector();
            while ((command = console.readLine()) != null && !command.isBlank()) {
                if ("offline".equals(command)) { connector.pause(); connector.stop(); System.out.println("SMOKE_HTTP_OFFLINE"); }
                else if ("online".equals(command)) { connector.start(); connector.resume(); System.out.println("SMOKE_HTTP_ONLINE"); }
                else { System.out.println("Unknown smoke command; use offline, online or Enter."); }
            }
            var jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
            int rows = jdbc.queryForObject("SELECT count(*) FROM moments", Integer.class);
            int recipients = jdbc.queryForObject("SELECT count(*) FROM moment_recipients", Integer.class);
            try (var files = Files.list(images)) {
                long count = files.count();
                System.out.println("SMOKE_MEDIA_ROWS=" + rows + " FILES=" + count + " RECIPIENTS=" + recipients);
                if (rows != count) { throw new IllegalStateException("Smoke media storage and metadata are inconsistent."); }
            }
        }
    }
}
