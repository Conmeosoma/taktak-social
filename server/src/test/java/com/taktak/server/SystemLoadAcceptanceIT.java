package com.taktak.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.storage.PrivateImageStorage;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** Load QA có giới hạn: luôn tự tạo HTTP loopback/DB/storage riêng, không nhận URL bên ngoài. */
@EnabledIfSystemProperty(named="taktak.load.qa", matches="true")
class SystemLoadAcceptanceIT {
    private static final AtomicLong STATEMENTS = new AtomicLong();
    @TestConfiguration(proxyBeanMethods=false)
    static class CountingDataSource {
        @Bean static BeanPostProcessor countStatements() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof DataSource source) || !name.equals("dataSource")) return bean;
                    return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class}, (p,m,a) -> {
                        Object result = invoke(source,m,a);
                        if (result instanceof Connection connection && m.getName().equals("getConnection")) {
                            return Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (cp,cm,ca) -> {
                                if (cm.getName().equals("prepareStatement") || cm.getName().equals("prepareCall")) STATEMENTS.incrementAndGet();
                                return invoke(connection,cm,ca);
                            });
                        }
                        return result;
                    });
                }
            };
        }
    }
    private static Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
        try { return method.invoke(target,arguments); }
        catch (InvocationTargetException exception) { throw exception.getCause(); }
    }
    record Sample(long nanos, int status) { }
    record Summary(int requests, int failures, double p50Ms, double p95Ms, double p99Ms,
                   double requestsPerSecond, long preparedStatements) { }

    @Test void boundedLoopbackWorkloadAndInboxQueryBudget() throws Exception {
        Path root=Files.createDirectories(Path.of("target","system-load-qa"));
        try (var pg=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile())
                .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start()) {
            byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
            try(var context=(ServletWebServerApplicationContext)new SpringApplication(TaktakApplication.class,CountingDataSource.class).run(
                    "--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
                    "--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key),
                    "--taktak.storage.root="+Files.createTempDirectory(root,"images-"),
                    "--server.address=127.0.0.1","--server.port=0","--debug=false","--logging.level.root=WARN",
                    "--taktak.fcm.enabled=false","--taktak.outbox.enabled=false")) {
                var jdbc=context.getBean(JdbcTemplate.class);
                UUID actor=account(jdbc);
                var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                String token=tx.execute(s -> context.getBean(TokenService.class).issue(actor).accessToken());
                byte[] image=jpeg(); UUID firstRoom=null,firstPhoto=null;
                for(int n=0;n<24;n++) {
                    UUID peer=account(jdbc), room=UUID.randomUUID();
                    jdbc.update("insert into friendships(user_low_id,user_high_id,created_at) values(least(?::uuid,?::uuid),greatest(?::uuid,?::uuid),now())",actor,peer,actor,peer);
                    jdbc.update("insert into conversations(id,user1_id,user2_id,created_at,updated_at) values(?,least(?::uuid,?::uuid),greatest(?::uuid,?::uuid),now(),now())",room,actor,peer,actor,peer);
                    UUID last=null;
                    for(int j=0;j<60;j++) {
                        last=UUID.randomUUID();
                        jdbc.update("insert into messages(id,conversation_id,sender_id,recipient_id,client_message_id,message_type,content,created_at) values(?,?,?,?,?,'TEXT','QA synthetic',now())",last,room,peer,actor,UUID.randomUUID());
                    }
                    jdbc.update("update conversations set last_message_id=? where id=?",last,room);
                    UUID photo=UUID.randomUUID();
                    String path=context.getBean(PrivateImageStorage.class).write(photo,image);
                    jdbc.update("insert into moments(id,sender_id,caption,file_path,mime_type,byte_size,created_at) values(?,?,'QA synthetic',?,'image/jpeg',?,now())",photo,peer,path,image.length);
                    jdbc.update("insert into moment_recipients(moment_id,user_id,created_at) values(?,?,now())",photo,actor);
                    if(firstRoom==null){firstRoom=room;firstPhoto=photo;}
                }
                jdbc.execute("analyze");
                String base="http://127.0.0.1:"+context.getWebServer().getPort();
                assertThat(URI.create(base).getHost()).isEqualTo("127.0.0.1");
                var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                var report=new LinkedHashMap<String,Object>();
                report.put("dataset",Map.of("users",25,"conversations",24,"messages",1440,"photos",24));
                report.put("fixtureOnly",true);report.put("concurrency",4);
                for(String route:List.of("/api/users/me","/api/conversations","/api/conversations/"+firstRoom+"/messages",
                        "/api/moments?type=received&page=0","/api/moments/"+firstPhoto+"/image")) {
                    for(int warm=0;warm<5;warm++) assertThat(request(http,base,route,token).status()).isEqualTo(200);
                    long before=STATEMENTS.get(), started=System.nanoTime();
                    var pool=Executors.newFixedThreadPool(4);
                    List<Sample> samples=new ArrayList<>();
                    try {
                        List<Callable<Sample>> jobs=new ArrayList<>();
                        for(int n=0;n<40;n++) jobs.add(() -> request(http,base,route,token));
                        for(var future:pool.invokeAll(jobs,90,TimeUnit.SECONDS)) samples.add(future.get());
                    } finally {pool.shutdownNow();}
                    double seconds=(System.nanoTime()-started)/1_000_000_000d;
                    long[] sorted=samples.stream().mapToLong(Sample::nanos).sorted().toArray();
                    int failed=(int)samples.stream().filter(s->s.status()!=200).count();
                    String label=route.contains("/messages")?"chatMessages":route.endsWith("/image")?"privateImage"
                            :route.startsWith("/api/moments?")?"receivedHistory":route.endsWith("/me")?"profile":"inbox";
                    report.put(label,new Summary(samples.size(),failed,percentile(sorted,.5),percentile(sorted,.95),percentile(sorted,.99),samples.size()/seconds,STATEMENTS.get()-before));
                    assertThat(failed).as("QA response failures for %s",label).isZero();
                    if(label.equals("inbox")) assertThat(STATEMENTS.get()-before)
                            .as("Text inbox query budget must not grow per conversation")
                            .isLessThanOrEqualTo(samples.size()*8L);
                }
                var writePool=Executors.newFixedThreadPool(4);
                int written=0;
                long writeStarted=System.nanoTime(), writeStatements=STATEMENTS.get();
                List<Sample> writes=new ArrayList<>();
                final String writeRoute="/api/conversations/"+firstRoom+"/messages";
                try {
                    List<Callable<List<Sample>>> jobs=new ArrayList<>();
                    for(int n=0;n<20;n++) {
                        String body=new ObjectMapper().writeValueAsString(Map.of("clientMessageId",UUID.randomUUID(),"messageType","TEXT","content","QA synthetic write"));
                        jobs.add(() -> List.of(request(http,base,writeRoute,token,body),request(http,base,writeRoute,token,body)));
                    }
                    for(var future:writePool.invokeAll(jobs,90,TimeUnit.SECONDS)) {writes.addAll(future.get());written++;}
                } finally {writePool.shutdownNow();}
                long[] writeSorted=writes.stream().mapToLong(Sample::nanos).sorted().toArray();
                int writeFailures=(int)writes.stream().filter(s->s.status()!=200).count();
                report.put("chatSendAndIdempotentRetry",new Summary(writes.size(),writeFailures,percentile(writeSorted,.5),percentile(writeSorted,.95),percentile(writeSorted,.99),
                        writes.size()/((System.nanoTime()-writeStarted)/1_000_000_000d),STATEMENTS.get()-writeStatements));
                assertThat(writeFailures).isZero();
                assertThat(jdbc.queryForObject("select count(*) from messages where sender_id=?",Integer.class,actor)).isEqualTo(written);
                // Không in token, UUID tài khoản, response body hoặc đường dẫn private.
                Path destination=root.resolve(System.getProperty("taktak.load.report","report.json"));
                assertThat(destination.normalize().getParent()).isEqualTo(root);
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(destination.toFile(),report);
                System.out.println("SYSTEM_LOAD_QA_REPORT="+destination);
                assertThat(request(http,base,"/api/users/me",null).status()).isEqualTo(401);
            }
        }
    }
    private static UUID account(JdbcTemplate jdbc) {
        UUID id=UUID.randomUUID();String suffix=id.toString().replace("-","");
        jdbc.update("insert into users(id,email,username,password_hash,display_name) values(?,?,?,'not-a-login-password','Load QA')",id,suffix+"@example.test","load_"+suffix);
        return id;
    }
    private static Sample request(HttpClient client,String base,String path,String token) throws Exception {
        return request(client,base,path,token,null);
    }
    private static Sample request(HttpClient client,String base,String path,String token,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(15));
        if(body==null) builder.GET();
        else builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if(token!=null) builder.header("Authorization","Bearer "+token);
        long start=System.nanoTime();
        var response=client.send(builder.build(),HttpResponse.BodyHandlers.discarding());
        return new Sample(System.nanoTime()-start,response.statusCode());
    }
    private static double percentile(long[] sorted,double fraction){return sorted[Math.max(0,(int)Math.ceil(sorted.length*fraction)-1)]/1_000_000d;}
    private static byte[] jpeg() throws Exception {
        var image=new BufferedImage(480,480,BufferedImage.TYPE_INT_RGB);
        try(var bytes=new ByteArrayOutputStream()){ImageIO.write(image,"jpeg",bytes);return bytes.toByteArray();}
    }
}
