package com.taktak.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class SchemaUpgradeCompatibilityTest {
    @Test void additiveV6PreservesRevocationAndExistingPhotoMessageData() throws Exception {
        Path root=Files.createDirectories(Path.of("target","upgrade-compatibility"));
        try(var pg=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start()) {
            var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(pg.getJdbcUrl("postgres","postgres"),"postgres","");
            Flyway.configure().dataSource(source).target("5").cleanDisabled(true).load().migrate();
            var jdbc=new JdbcTemplate(source);
            UUID actor=UUID.randomUUID(),peer=UUID.randomUUID(),photo=UUID.randomUUID(),room=UUID.randomUUID(),message=UUID.randomUUID();
            for(UUID id:java.util.List.of(actor,peer)) jdbc.update("insert into users(id,email,username,password_hash,display_name,security_version) values(?,?,?,'QA hash','QA',7)",id,id+"@example.test","qa_"+id.toString().replace("-",""));
            jdbc.update("insert into refresh_tokens(id,user_id,token_hash,expires_at,revoked_at,created_at) values(?,?,'QA non-credential hash',now()+interval '1 day',now(),now())",UUID.randomUUID(),actor);
            jdbc.update("insert into moments(id,sender_id,caption,file_path,mime_type,byte_size,created_at) values(?,?,'QA',?,'image/jpeg',1,now())",photo,peer,photo+".jpg");
            jdbc.update("insert into moment_recipients(moment_id,user_id,created_at) values(?,?,now())",photo,actor);
            jdbc.update("insert into conversations(id,user1_id,user2_id,created_at,updated_at) values(?,least(?::uuid,?::uuid),greatest(?::uuid,?::uuid),now(),now())",room,actor,peer,actor,peer);
            jdbc.update("insert into messages(id,conversation_id,sender_id,recipient_id,client_message_id,message_type,content,moment_id,created_at) values(?,?,?,?,?,'MOMENT_REPLY','QA retained reply',?,now())",message,room,actor,peer,UUID.randomUUID(),photo);
            var latest=Flyway.configure().dataSource(source).cleanDisabled(true).load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
            latest.validate();
            assertThat(latest.migrate().migrationsExecuted).isZero();
            assertThat(jdbc.queryForObject("select min(security_version) from users",Long.class)).isEqualTo(7);
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where revoked_at is not null",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select content from messages where id=?",String.class,message)).isEqualTo("QA retained reply");
            assertThat(jdbc.queryForObject("select count(*) from moment_recipients",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from push_outbox",Integer.class)).isZero();
        }
    }
}
