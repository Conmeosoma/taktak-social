package com.taktak.server;

import com.taktak.server.auth.TokenService;
import com.taktak.server.storage.PrivateImageStorage;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

/** Acceptance assertions: không nới kỳ vọng khi tìm thấy lỗi bảo mật. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdvancedFeaturesQaTest {
    EmbeddedPostgres pg; ServletWebServerApplicationContext app; JdbcTemplate db; String base;
    @BeforeAll void start() throws Exception {
        Path root=Files.createDirectories(Path.of("target","advanced4-qa"));
        pg=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"db-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setPort(0).start();
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        app=(ServletWebServerApplicationContext)SpringApplication.run(TaktakApplication.class,
                "--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres",
                "--spring.datasource.password=","--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key),
                "--taktak.storage.root="+Files.createTempDirectory(root,"media-"),"--server.port=0",
                "--server.address=127.0.0.1","--logging.level.root=WARN");
        db=app.getBean(JdbcTemplate.class);base="http://127.0.0.1:"+app.getWebServer().getPort();
    }
    @AfterAll void stop() throws Exception {try{if(app!=null)app.close();}finally{if(pg!=null)pg.close();}}
    UUID account(){UUID id=UUID.randomUUID();db.update("insert into users(id,email,username,password_hash,display_name) values(?,?,?,?,?)",
            id,id+"@example.test","qa_"+id.toString().replace("-",""),"not-a-login-hash","QA");return id;}
    UUID photo(UUID owner){UUID id=UUID.randomUUID();db.update("insert into moments(id,sender_id,caption,file_path,mime_type,byte_size,created_at) values(?,?,'QA',?,'image/jpeg',100,now())",id,owner,id+".jpg");return id;}
    HttpResponse<String> create(UUID actor,UUID photo)throws Exception{
        String token=new TransactionTemplate(app.getBean(PlatformTransactionManager.class)).execute(s->app.getBean(TokenService.class).issue(actor).accessToken());
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/api/moments/"+photo+"/poll"))
                .header("Authorization","Bearer "+token).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"QA?\",\"optionA\":\"A\",\"optionB\":\"B\"}")).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void ownerCanCreatePoll()throws Exception{UUID actor=account();assertThat(create(actor,photo(actor)).statusCode()).isEqualTo(201);}
    @Test void liveUploadUsesDecodedMetadataInsteadOfClientClaims() throws Exception {
        byte[] video;
        try (var input=getClass().getResourceAsStream("/live-video/audio-3s.mp4")) { video=input.readAllBytes(); }
        var output=new java.io.ByteArrayOutputStream();
        var bitmap=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB);
        try { javax.imageio.ImageIO.write(bitmap,"jpeg",output); } finally { bitmap.flush(); }
        UUID id=app.getBean(com.taktak.server.moment.MomentService.class).uploadLive(account(),
                new MockMultipartFile("image","thumb.jpg","image/jpeg",output.toByteArray()),
                new MockMultipartFile("video","live.mp4","video/mp4",video),"QA",List.of(),null,1,false);
        assertThat(db.queryForObject("select duration_ms from moments where id=?",Integer.class,id)).isBetween(3000,3100);
        assertThat(db.queryForObject("select has_audio from moments where id=?",Boolean.class,id)).isTrue();
    }
    @Test void unrelatedAccountCannotPersistPollEvenWhenResponseDeniesAccess()throws Exception{
        UUID owner=account(),stranger=account(),photo=photo(owner);var response=create(stranger,photo);
        assertThat(response.statusCode()).isIn(403,404);
        assertThat(db.queryForObject("select count(*) from moment_polls where moment_id=?",Integer.class,photo))
                .as("Unauthorized request must not mutate owner photo").isZero();
    }
    @Test void recipientCannotCreatePollOnSomeoneElsesPhoto()throws Exception{
        UUID owner=account(),recipient=account(),photo=photo(owner);
        db.update("insert into moment_recipients(moment_id,user_id,created_at) values(?,?,now())",photo,recipient);
        assertThat(create(recipient,photo).statusCode()).as("Only sender owns poll creation").isIn(403,404);
    }
    @Test void retryCreatesExactlyOnePollAndPreservesId()throws Exception{
        UUID owner=account(),id=photo(owner);
        var first=create(owner,id);var second=create(owner,id);
        assertThat(first.statusCode()).isEqualTo(201);assertThat(second.statusCode()).isEqualTo(201);
        assertThat(second.body()).isEqualTo(first.body());
        assertThat(db.queryForObject("select count(*) from moment_polls where moment_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void simultaneousSamePayloadCreatesOnePoll()throws Exception{
        UUID owner=account(),id=photo(owner);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->create(owner,id));var b=pool.submit(()->create(owner,id));
            assertThat(a.get().statusCode()).isEqualTo(201);assertThat(b.get().statusCode()).isEqualTo(201);
            assertThat(db.queryForObject("select count(*) from moment_polls where moment_id=?",Integer.class,id)).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void fakeEightByteMp4MustBeRejected() {
        byte[] fake=new byte[]{0,0,0,8,'f','t','y','p'};
        assertThatThrownBy(()->app.getBean(PrivateImageStorage.class).sanitizeVideo(new MockMultipartFile("video","fake.mp4","video/mp4",fake)))
                .as("MP4 signature alone does not establish decodable video or a 3-second duration").isInstanceOf(Exception.class);
    }
}
