package com.taktak.server;

import com.taktak.server.auth.TokenService;
import com.taktak.server.user.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvatarIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private Path images;
    private String base;
    private final HttpClient http=HttpClient.newHttpClient();
    @BeforeAll void start() throws Exception {
        var root=Files.createDirectories(Path.of("target","avatar-tests"));images=Files.createTempDirectory(root,"images-");
        postgres=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setServerConfig("listen_addresses","127.0.0.1")
                .setServerConfig("track_activities","on").setPort(0).start();
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        context=(ServletWebServerApplicationContext)SpringApplication.run(TaktakApplication.class,
                "--spring.datasource.url="+postgres.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
                "--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key),"--taktak.storage.root="+images.toAbsolutePath(),
                "--server.port=0","--server.address=127.0.0.1","--debug=false","--logging.level.root=WARN");
        base="http://127.0.0.1:"+context.getWebServer().getPort();
    }
    @AfterAll void stop() throws Exception { try { if(context!=null)context.close(); } finally {if(postgres!=null)postgres.close();} }
    private record Account(UUID id,String token) {}
    private Account account() {
        String name="avatar_"+UUID.randomUUID().toString().replace("-","");
        var user=context.getBean(UserRepository.class).saveAndFlush(new User(name+"@example.test",name,"fixture-unused-hash","Avatar QA"));
        return new Account(user.getId(),context.getBean(TokenService.class).issue(user.getId()).accessToken());
    }
    private byte[] jpeg() throws IOException {
        var bitmap=new BufferedImage(64,32,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();
        ImageIO.write(bitmap,"jpeg",output);bitmap.flush();return output.toByteArray();
    }
    private HttpResponse<byte[]> get(Account actor,String path) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+path));if(actor!=null)request.header("Authorization","Bearer "+actor.token);
        return http.send(request.GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    private HttpResponse<byte[]> upload(Account actor,byte[] bytes) throws Exception {
        String boundary="fixture"+UUID.randomUUID();var payload=new ByteArrayOutputStream();
        payload.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"avatar.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        payload.write(bytes);payload.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var request=HttpRequest.newBuilder(URI.create(base+"/api/users/me/avatar")).header("Content-Type","multipart/form-data; boundary="+boundary);
        if(actor!=null)request.header("Authorization","Bearer "+actor.token);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(payload.toByteArray())).build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    private String path(Account actor) { return context.getBean(UserRepository.class).findById(actor.id).orElseThrow().getAvatarPath(); }
    @Test void uploadDownloadsSquareJpegAndDoesNotExposeStoragePath() throws Exception {
        var actor=account();assertThat(get(actor,"/api/users/me/avatar").statusCode()).isEqualTo(204);
        var uploaded=upload(actor,jpeg());assertThat(uploaded.statusCode()).isEqualTo(200);
        assertThat(new String(uploaded.body(),StandardCharsets.UTF_8)).doesNotContain("avatarPath","passwordHash");
        var downloaded=get(actor,"/api/users/me/avatar");assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.headers().firstValue("Cache-Control")).contains("no-store");
        var decoded=ImageIO.read(new ByteArrayInputStream(downloaded.body()));assertThat(decoded.getWidth()).isEqualTo(512);assertThat(decoded.getHeight()).isEqualTo(512);decoded.flush();
        assertThat(Files.isRegularFile(images.resolve("avatars").resolve(path(actor)))).isTrue();
    }
    @Test void authenticationAndForeignAccessAreEnforced() throws Exception {
        var owner=account();var stranger=account();upload(owner,jpeg());
        assertThat(get(null,"/api/users/"+owner.id+"/avatar").statusCode()).isEqualTo(401);
        assertThat(upload(null,jpeg()).statusCode()).isEqualTo(401);
        assertThat(get(stranger,"/api/users/"+owner.id+"/avatar").statusCode()).isEqualTo(404);
    }
    @Test void acceptedFriendCanSeeAvatarButBlockRevokesAccess() throws Exception {
        var owner=account();var friend=account();upload(owner,jpeg());var jdbc=context.getBean(JdbcTemplate.class);
        jdbc.update("INSERT INTO friendships(user_low_id,user_high_id) VALUES(LEAST(?::uuid,?::uuid),GREATEST(?::uuid,?::uuid))",owner.id,friend.id,owner.id,friend.id);
        assertThat(get(friend,"/api/users/"+owner.id+"/avatar").statusCode()).isEqualTo(200);
        jdbc.update("INSERT INTO blocks(blocker_id,blocked_id) VALUES(?,?)",owner.id,friend.id);
        assertThat(get(friend,"/api/users/"+owner.id+"/avatar").statusCode()).isEqualTo(404);
    }
    @Test void malformedAndOversizedUploadsLeaveExistingAvatarUntouched() throws Exception {
        var actor=account();upload(actor,jpeg());String original=path(actor);
        assertThat(upload(actor,"not-a-jpeg".getBytes(StandardCharsets.UTF_8)).statusCode()).isEqualTo(415);
        assertThat(upload(actor,new byte[5*1024*1024+1]).statusCode()).isEqualTo(413);
        assertThat(path(actor)).isEqualTo(original);assertThat(Files.exists(images.resolve("avatars").resolve(original))).isTrue();
    }
    @Test void replacementCommitCleansOldFileAndRollbackKeepsIt() throws Exception {
        var actor=account();upload(actor,jpeg());String original=path(actor);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            try {context.getBean(AvatarService.class).upload(actor.id,new MockMultipartFile("image","avatar.jpg","image/jpeg",jpeg()));}
            catch(IOException e){throw new UncheckedIOException(e);}
            assertThat(Files.exists(images.resolve("avatars").resolve(original))).isTrue();status.setRollbackOnly();
        });
        assertThat(path(actor)).isEqualTo(original);assertThat(Files.exists(images.resolve("avatars").resolve(original))).isTrue();
        upload(actor,jpeg());assertThat(path(actor)).isNotEqualTo(original);assertThat(Files.exists(images.resolve("avatars").resolve(original))).isFalse();
    }
    @Test void concurrentNameUpdateDoesNotOverwriteNewAvatarReference() throws Exception {
        var actor=account();var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        var update=new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<UserProfileResponse>>();
        try {
            var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            transaction.executeWithoutResult(status -> {
                context.getBean(UserRepository.class).lockAll(List.of(actor.id));
                update.set(executor.submit(() -> context.getBean(UserService.class).updateProfile(actor.id,new UpdateProfileRequest("Concurrent name"))));
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);boolean waiting=false;
                while(System.nanoTime()<deadline) {
                    if(update.get().isDone()) {
                        try {update.get().get();}catch(Exception failure){throw new AssertionError("Concurrent mutation failed before acquiring lock",failure);}
                        throw new AssertionError("Concurrent mutation did not wait for lock");
                    }
                    context.getBean(JdbcTemplate.class).execute("SELECT pg_stat_clear_snapshot()");
                    Integer count=context.getBean(JdbcTemplate.class).queryForObject(
                            "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE '%users%'",Integer.class);
                    if(count!=null && count>0){waiting=true;break;}
                    try{Thread.sleep(20);}catch(InterruptedException failure){throw new AssertionError(failure);}
                }
                assertThat(waiting).as("Concurrent profile mutation waits on user lock").isTrue();
                try{context.getBean(AvatarService.class).upload(actor.id,new MockMultipartFile("image","avatar.jpg","image/jpeg",jpeg()));}
                catch(IOException failure){throw new UncheckedIOException(failure);}
            });
            assertThat(update.get().get(5,java.util.concurrent.TimeUnit.SECONDS).displayName()).isEqualTo("Concurrent name");
            assertThat(path(actor)).isNotNull();assertThat(get(actor,"/api/users/me/avatar").statusCode()).isEqualTo(200);
        } finally {executor.shutdownNow();}
    }
}
