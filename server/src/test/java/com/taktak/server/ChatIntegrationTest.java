package com.taktak.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.auth.TokenService;
import com.taktak.server.chat.ChatDtos.SendRequest;
import com.taktak.server.chat.ChatService;
import com.taktak.server.friend.FriendService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatIntegrationTest {
    private EmbeddedPostgres postgres;
    private ServletWebServerApplicationContext context;
    private JdbcTemplate jdbc;
    private String base;
    private RecordingPushSender push;
    private final ObjectMapper json=new ObjectMapper();
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class PushFixture {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        RecordingPushSender chatPushFixture() { return new RecordingPushSender(); }
    }
    @BeforeAll void start() throws Exception {
        Path root=Files.createDirectories(Path.of("target","chat-tests"));
        postgres=EmbeddedPostgres.builder().setDataDirectory(Files.createTempDirectory(root,"cluster-"))
                .setOverrideWorkingDirectory(root.resolve("binaries").toFile()).setServerConfig("listen_addresses","127.0.0.1").setPort(0).start();
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        context=(ServletWebServerApplicationContext)new SpringApplication(TaktakApplication.class,PushFixture.class).run(
                "--spring.datasource.url="+postgres.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=",
                "--taktak.jwt.access-secret="+Base64.getEncoder().encodeToString(key),"--taktak.storage.root="+Files.createTempDirectory(root,"images-"),
                "--server.port=0","--server.address=127.0.0.1","--debug=false","--logging.level.root=WARN","--taktak.outbox.enabled=false");
        jdbc=context.getBean(JdbcTemplate.class);push=context.getBean(RecordingPushSender.class);
        base="http://127.0.0.1:"+context.getWebServer().getPort();
    }
    @AfterAll void stop() throws Exception { try { if(context!=null)context.close(); } finally {if(postgres!=null)postgres.close();} }
    @BeforeEach void resetPush() { recorded();push.clearRecordedMessages(); }
    private java.util.List<RecordingPushSender.SentMessage> recorded() {
        while(context.getBean(com.taktak.server.outbox.PushOutboxDispatcher.class).dispatchBatch()==25) { }
        return push.getRecordedMessages();
    }
    record Account(UUID id,String token) { }
    private Account account() {
        UUID id=UUID.randomUUID();String suffix=id.toString().replace("-","");
        jdbc.update("insert into users(id,email,username,password_hash,display_name) values(?,?,?,?,?)",id,suffix+"@example.test","chat_"+suffix,"not-a-login-password","Chat QA");
        String token=transaction().execute(s->context.getBean(TokenService.class).issue(id).accessToken());return new Account(id,token);
    }
    private TransactionTemplate transaction() { return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)); }
    private void friends(Account a,Account b) {
        jdbc.update("insert into friendships(user_low_id,user_high_id,created_at) values(least(?::uuid,?::uuid),greatest(?::uuid,?::uuid),now())",a.id,b.id,a.id,b.id);
    }
    private HttpResponse<String> call(String method,String path,Account actor,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(15));
        if(actor!=null)builder.header("Authorization","Bearer "+actor.token);
        if(body!=null)builder.header("Content-Type","application/json");
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private UUID open(Account a,Account b) throws Exception {
        var response=call("POST","/api/conversations/with-friend/"+b.id,a,null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return UUID.fromString(json.readTree(response.body()).get("id").asText());
    }
    private String body(UUID key,String text) throws Exception {return json.writeValueAsString(Map.of("clientMessageId",key,"messageType","TEXT","content",text));}
    private HttpResponse<String> send(Account a,UUID conversation,UUID key,String text) throws Exception {
        return call("POST","/api/conversations/"+conversation+"/messages",a,body(key,text));
    }
    @Test void canonicalPairAndConcurrentOpenAreUnique() throws Exception {
        var a=account();var b=account();friends(a,b);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->open(a,b));var second=pool.submit(()->open(b,a));
            assertThat(first.get(20,TimeUnit.SECONDS)).isEqualTo(second.get(20,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from conversations where user1_id=least(?::uuid,?::uuid) and user2_id=greatest(?::uuid,?::uuid)",Integer.class,a.id,b.id,a.id,b.id)).isEqualTo(1);
    }
    @Test void batchedInboxPreservesPeersLastMessagesUnreadPaginationAndBlock() throws Exception {
        var actor=account();
        Account newest=null;
        for(int n=0;n<21;n++) {
            var peer=account();friends(actor,peer);UUID room=open(actor,peer);
            assertThat(send(peer,room,UUID.randomUUID(),"QA inbox "+n).statusCode()).isEqualTo(200);
            newest=peer;
        }
        var first=json.readTree(call("GET","/api/conversations",actor,null).body());
        assertThat(first.path("items").size()).isEqualTo(20);
        assertThat(first.path("hasNext").asBoolean()).isTrue();
        assertThat(first.path("totalUnreadCount").asInt()).isEqualTo(21);
        for(var item:first.path("items")) {
            assertThat(item.path("peer").path("id").asText()).isEqualTo(item.path("lastMessage").path("senderId").asText());
            assertThat(item.path("peer").path("username").asText()).startsWith("chat_");
            assertThat(item.path("lastMessage").path("content").asText()).startsWith("QA inbox ");
            assertThat(item.path("unreadCount").asInt()).isEqualTo(1);
        }
        var second=json.readTree(call("GET","/api/conversations?page=1",actor,null).body());
        assertThat(second.path("items").size()).isEqualTo(1);
        assertThat(second.path("hasNext").asBoolean()).isFalse();
        context.getBean(FriendService.class).block(actor.id,newest.id);
        var filtered=json.readTree(call("GET","/api/conversations",actor,null).body());
        assertThat(filtered.path("items").size()).isEqualTo(20);
        assertThat(filtered.path("hasNext").asBoolean()).isFalse();
        assertThat(filtered.path("totalUnreadCount").asInt()).isEqualTo(20);
        for(var item:filtered.path("items")) assertThat(item.path("peer").path("id").asText()).isNotEqualTo(newest.id.toString());
    }
    @Test void strangersSelfAnonymousAndForeignConversationAreDenied() throws Exception {
        var a=account();var b=account();var c=account();friends(a,b);UUID conversation=open(a,b);
        assertThat(call("POST","/api/conversations/with-friend/"+a.id,a,null).statusCode()).isEqualTo(400);
        assertThat(call("POST","/api/conversations/with-friend/"+c.id,a,null).statusCode()).isEqualTo(404);
        assertThat(call("GET","/api/conversations",null,null).statusCode()).isEqualTo(401);
        assertThat(call("GET","/api/conversations/"+conversation,c,null).statusCode()).isEqualTo(404);
        assertThat(call("GET","/api/conversations/"+conversation+"/messages",c,null).statusCode()).isEqualTo(404);
        assertThat(send(c,conversation,UUID.randomUUID(),"private").statusCode()).isEqualTo(404);
    }
    @Test void textSendUnreadReadCutoffAndFutureMessages() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        var first=send(a,conversation,UUID.randomUUID(),"Xin chào 👋");assertThat(first.statusCode()).isEqualTo(200);
        UUID anchor=UUID.fromString(json.readTree(first.body()).get("id").asText());
        assertThat(send(a,conversation,UUID.randomUUID(),"Later").statusCode()).isEqualTo(200);
        String path="/api/conversations/"+conversation;
        assertThat(json.readTree(call("GET",path,b,null).body()).get("unreadCount").asInt()).isEqualTo(2);
        assertThat(call("PUT",path+"/read",b,json.writeValueAsString(Map.of("messageId",anchor))).statusCode()).isEqualTo(204);
        var messages=json.readTree(call("GET",path+"/messages",b,null).body()).get("items");
        assertThat(messages.get(0).get("readAt").isNull()).isTrue();assertThat(messages.get(1).get("readAt").isNull()).isFalse();
        assertThat(send(a,conversation,UUID.randomUUID(),"Future").statusCode()).isEqualTo(200);
        assertThat(json.readTree(call("GET",path,b,null).body()).get("unreadCount").asInt()).isEqualTo(2);
        assertThat(call("GET",path,a,null).headers().firstValue("Cache-Control")).hasValue("no-store");
    }
    @Test void idempotencyRetryAndConcurrentRequestsProduceOneMessageOnePush() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);UUID key=UUID.randomUUID();
        jdbc.update("insert into device_tokens(id,user_id,fcm_token,device_id,updated_at) values(?,?,?,?,now())",UUID.randomUUID(),b.id,"synthetic-chat-device","qa");
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->send(a,conversation,key,"hello"));var second=pool.submit(()->send(a,conversation,key,"hello"));
            var one=first.get(20,TimeUnit.SECONDS);var two=second.get(20,TimeUnit.SECONDS);
            assertThat(one.statusCode()).isEqualTo(200);assertThat(two.statusCode()).isEqualTo(200);
            assertThat(json.readTree(one.body()).get("id")).isEqualTo(json.readTree(two.body()).get("id"));
        } finally {pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from messages where sender_id=? and client_message_id=?",Integer.class,a.id,key)).isEqualTo(1);
        assertThat(send(a,conversation,key,"changed").statusCode()).isEqualTo(409);
        assertThat(recorded()).hasSize(1);
        assertThat(recorded().get(0).data()).containsOnlyKeys("type","conversationId","messageId").containsEntry("type","chat_message");
    }
    @Test void invalidBodiesUnicodeLimitNulAndPaginationAreRejected() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        for(String text:List.of("","  ","x".repeat(2001),"\0")) assertThat(send(a,conversation,UUID.randomUUID(),text).statusCode()).isEqualTo(400);
        assertThat(send(a,conversation,UUID.randomUUID(),"😀".repeat(2000)).statusCode()).isEqualTo(200);
        for(String bad:List.of("{}","null","{\"clientMessageId\":\"bad\"}","[]"))
            assertThat(call("POST","/api/conversations/"+conversation+"/messages",a,bad).statusCode()).isEqualTo(400);
        assertThat(call("GET","/api/conversations?page=-1",a,null).statusCode()).isEqualTo(400);
        assertThat(call("GET","/api/conversations/"+conversation+"/messages?page=10001",a,null).statusCode()).isEqualTo(400);
        assertThat(new SendRequest(UUID.randomUUID(),"TEXT","private content",null).toString()).doesNotContain("private content");
        assertThat(context.getBean(ChatService.class).send(a.id,conversation,new SendRequest(UUID.randomUUID(),"TEXT","private content",null)).toString()).doesNotContain("private content");
    }
    @Test void blockBothDirectionsAndUnfriendHideExistingConversations() throws Exception {
        for(boolean reverse:List.of(false,true)) {
            var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);send(a,conversation,UUID.randomUUID(),"hello");
            context.getBean(FriendService.class).block(reverse?b.id:a.id,reverse?a.id:b.id);
            assertThat(send(a,conversation,UUID.randomUUID(),"blocked").statusCode()).isEqualTo(404);
            assertThat(call("GET","/api/conversations/"+conversation+"/messages",b,null).statusCode()).isEqualTo(404);
            assertThat(json.readTree(call("GET","/api/conversations",a,null).body()).get("items")).isEmpty();
        }
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        context.getBean(FriendService.class).unfriend(a.id,b.id);
        assertThat(call("GET","/api/conversations/"+conversation,a,null).statusCode()).isEqualTo(404);
    }
    private UUID moment(Account owner,Account recipient) {
        UUID id=UUID.randomUUID();
        jdbc.update("insert into moments(id,sender_id,caption,file_path,mime_type,byte_size,created_at) values(?,?,?,?,?,?,now())",id,owner.id,"Private caption",id+".jpg","image/jpeg",1);
        if(recipient!=null)jdbc.update("insert into moment_recipients(moment_id,user_id,created_at) values(?,?,now())",id,recipient.id);
        return id;
    }
    private HttpResponse<String> momentSend(Account a,UUID conversation,UUID moment) throws Exception {
        return call("POST","/api/conversations/"+conversation+"/messages",a,json.writeValueAsString(Map.of("clientMessageId",UUID.randomUUID(),"messageType","MOMENT","momentId",moment)));
    }
    private HttpResponse<String> reply(Account actor,UUID conversation,UUID key,UUID moment,String text) throws Exception {
        Map<String,Object> payload=new HashMap<>();
        payload.put("clientMessageId",key);payload.put("messageType","MOMENT_REPLY");
        payload.put("momentId",moment);payload.put("content",text);
        return call("POST","/api/conversations/"+conversation+"/messages",actor,json.writeValueAsString(payload));
    }
    @Test void momentReplyIsPrivateChatTextWithReferenceUnreadAndRead() throws Exception {
        var owner=account();var viewer=account();var outsider=account();friends(owner,viewer);
        UUID conversation=open(viewer,owner), photo=moment(owner,viewer);
        var response=reply(viewer,conversation,UUID.randomUUID(),photo,"Đẹp quá 📷");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        var message=json.readTree(response.body());
        assertThat(message.get("messageType").asText()).isEqualTo("MOMENT_REPLY");
        assertThat(message.get("content").asText()).isEqualTo("Đẹp quá 📷");
        assertThat(message.get("momentId").asText()).isEqualTo(photo.toString());
        assertThat(response.body()).doesNotContain("Private caption","filePath",".jpg");
        String path="/api/conversations/"+conversation;
        assertThat(json.readTree(call("GET",path+"/messages",owner,null).body()).get("items").get(0)).isEqualTo(message);
        assertThat(json.readTree(call("GET",path,owner,null).body()).get("unreadCount").asInt()).isEqualTo(1);
        var inbox=json.readTree(call("GET","/api/conversations",owner,null).body());
        assertThat(inbox.get("items").get(0).get("lastMessage").get("content").asText()).isEqualTo("Đẹp quá 📷");
        assertThat(call("PUT",path+"/read",owner,json.writeValueAsString(Map.of("messageId",message.get("id").asText()))).statusCode()).isEqualTo(204);
        assertThat(json.readTree(call("GET",path,owner,null).body()).get("unreadCount").asInt()).isZero();
        assertThat(call("GET",path+"/messages",outsider,null).statusCode()).isEqualTo(404);
        assertThat(reply(outsider,conversation,UUID.randomUUID(),photo,"private").statusCode()).isEqualTo(404);
    }
    @Test void momentReplyRequiresOtherOwnerAndExistingGrantNeverSharesWithThirdParty() throws Exception {
        var owner=account();var viewer=account();var other=account();friends(owner,viewer);friends(viewer,other);
        UUID conversation=open(viewer,owner), otherConversation=open(viewer,other), photo=moment(owner,viewer);
        assertThat(reply(viewer,otherConversation,UUID.randomUUID(),photo,"wrong owner").statusCode()).isEqualTo(404);
        // Even if both chat participants can view a third person's image, reply only goes to its owner.
        jdbc.update("insert into moment_recipients(moment_id,user_id,created_at) values(?,?,now())",photo,other.id);
        assertThat(reply(viewer,otherConversation,UUID.randomUUID(),photo,"wrong owner").statusCode()).isEqualTo(404);
        assertThat(reply(owner,conversation,UUID.randomUUID(),photo,"own photo").statusCode()).isEqualTo(404);
        assertThat(reply(viewer,conversation,UUID.randomUUID(),moment(owner,null),"private photo").statusCode()).isEqualTo(404);
        assertThat(reply(viewer,conversation,UUID.randomUUID(),UUID.randomUUID(),"missing").statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("select count(*) from messages where conversation_id in (?,?)",Integer.class,conversation,otherConversation)).isZero();
    }
    @Test void momentReplyValidationUnicodeLimitsAndLegacyMomentRemainStrict() throws Exception {
        var owner=account();var viewer=account();friends(owner,viewer);UUID conversation=open(viewer,owner), photo=moment(owner,viewer);
        for(String text:Arrays.asList(null,""," \t\n","\0","x".repeat(2001),"😀".repeat(2001)))
            assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,text).statusCode()).as("invalid reply").isEqualTo(400);
        assertThat(reply(viewer,conversation,UUID.randomUUID(),null,"missing reference").statusCode()).isEqualTo(400);
        assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,"😀".repeat(2000)).statusCode()).isEqualTo(200);
        assertThat(call("POST","/api/conversations/"+conversation+"/messages",viewer,json.writeValueAsString(Map.of(
                "clientMessageId",UUID.randomUUID(),"messageType","MOMENT_REPLY","momentId","invalid","content","hello"))).statusCode()).isEqualTo(400);
        assertThat(call("POST","/api/conversations/"+conversation+"/messages",viewer,json.writeValueAsString(Map.of(
                "clientMessageId",UUID.randomUUID(),"messageType","MOMENT","momentId",photo,"content","not allowed"))).statusCode()).isEqualTo(400);
        assertThat(momentSend(viewer,conversation,photo).statusCode()).isEqualTo(200);
    }
    @Test void momentReplyRetryIsStrictAndConcurrentOnlyPublishesOneSafePush() throws Exception {
        var owner=account();var viewer=account();var other=account();friends(owner,viewer);friends(viewer,other);
        UUID conversation=open(viewer,owner), key=UUID.randomUUID(), photo=moment(owner,viewer);
        jdbc.update("insert into device_tokens(id,user_id,fcm_token,device_id,updated_at) values(?,?,?,?,now())",UUID.randomUUID(),owner.id,"synthetic-reply-device-"+key,"qa");
        var pool=Executors.newFixedThreadPool(2);
        try {
            var one=pool.submit(()->reply(viewer,conversation,key,photo,"secret reply"));
            var two=pool.submit(()->reply(viewer,conversation,key,photo,"secret reply"));
            var first=one.get(20,TimeUnit.SECONDS);var second=two.get(20,TimeUnit.SECONDS);
            assertThat(first.statusCode()).isEqualTo(200);assertThat(second.statusCode()).isEqualTo(200);
            assertThat(json.readTree(first.body()).get("id")).isEqualTo(json.readTree(second.body()).get("id"));
        } finally {pool.shutdownNow();}
        assertThat(reply(viewer,conversation,key,photo,"changed").statusCode()).isEqualTo(409);
        assertThat(reply(viewer,conversation,key,moment(owner,viewer),"secret reply").statusCode()).isEqualTo(409);
        assertThat(send(viewer,conversation,key,"secret reply").statusCode()).isEqualTo(409);
        assertThat(reply(viewer,open(viewer,other),key,photo,"secret reply").statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("select count(*) from messages where sender_id=? and client_message_id=?",Integer.class,viewer.id,key)).isEqualTo(1);
        assertThat(recorded()).hasSize(1);
        assertThat(recorded().get(0).data()).containsOnlyKeys("type","conversationId","messageId");
    }
    @Test void deletedOrRevokedReplyReferenceIsRedactedButPrivateTextRemains() throws Exception {
        for(boolean deleted:List.of(false,true)) {
            var owner=account();var viewer=account();friends(owner,viewer);
            UUID conversation=open(viewer,owner),key=UUID.randomUUID(),photo=moment(owner,viewer);
            assertThat(reply(viewer,conversation,key,photo,"keep this reply").statusCode()).isEqualTo(200);
            if(deleted) jdbc.update("update moments set deleted_at=now() where id=?",photo);
            else jdbc.update("delete from moment_recipients where moment_id=? and user_id=?",photo,viewer.id);
            String path="/api/conversations/"+conversation;
            var redacted=json.readTree(call("GET",path+"/messages",owner,null).body()).get("items").get(0);
            assertThat(redacted.get("momentId").isNull()).isTrue();
            assertThat(redacted.get("content").asText()).isEqualTo("keep this reply");
            assertThat(json.readTree(call("GET",path,viewer,null).body()).get("lastMessage").get("momentId").isNull()).isTrue();
            var retry=reply(viewer,conversation,key,photo,"keep this reply");
            assertThat(retry.statusCode()).isEqualTo(200);
            assertThat(json.readTree(retry.body()).get("momentId").isNull()).isTrue();
            assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,"new forbidden").statusCode()).isEqualTo(404);
        }
    }
    @Test void momentReplyIsDeniedAfterBlockBothDirectionsOrUnfriend() throws Exception {
        for(int action=0;action<3;action++) {
            var owner=account();var viewer=account();friends(owner,viewer);UUID conversation=open(viewer,owner),photo=moment(owner,viewer);
            if(action==0) context.getBean(FriendService.class).block(owner.id,viewer.id);
            else if(action==1) context.getBean(FriendService.class).block(viewer.id,owner.id);
            else context.getBean(FriendService.class).unfriend(owner.id,viewer.id);
            assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,"blocked").statusCode()).isEqualTo(404);
        }
    }
    @Test void momentReplyRollbackAndPhysicalDeletionKeepPrivateTextWithoutGrant() throws Exception {
        var owner=account();var viewer=account();friends(owner,viewer);
        UUID conversation=open(viewer,owner),photo=moment(owner,viewer);
        jdbc.update("insert into device_tokens(id,user_id,fcm_token,device_id,updated_at) values(?,?,?,?,now())",UUID.randomUUID(),owner.id,"synthetic-reply-rollback-"+photo,"qa");
        transaction().executeWithoutResult(status->{
            context.getBean(ChatService.class).send(viewer.id,conversation,new SendRequest(UUID.randomUUID(),"MOMENT_REPLY","rollback",photo));
            status.setRollbackOnly();
        });
        assertThat(recorded()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from messages where conversation_id=?",Integer.class,conversation)).isZero();
        assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,"private reply survives").statusCode()).isEqualTo(200);
        jdbc.update("delete from moment_recipients where moment_id=?",photo);
        jdbc.update("delete from moments where id=?",photo);
        var message=json.readTree(call("GET","/api/conversations/"+conversation+"/messages",owner,null).body()).get("items").get(0);
        assertThat(message.get("momentId").isNull()).isTrue();
        assertThat(message.get("content").asText()).isEqualTo("private reply survives");
        assertThat(reply(viewer,conversation,UUID.randomUUID(),photo,"new forbidden").statusCode()).isEqualTo(404);
    }
    @Test void momentRequiresBothGrantsAndDeletedMomentIsRedacted() throws Exception {
        var a=account();var b=account();var c=account();friends(a,b);UUID conversation=open(a,b);
        assertThat(momentSend(a,conversation,moment(a,null)).statusCode()).isEqualTo(404);
        assertThat(momentSend(a,conversation,moment(c,a)).statusCode()).isEqualTo(404);
        UUID shared=moment(a,b);var response=momentSend(a,conversation,shared);assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Private caption").doesNotContain("filePath",".jpg","email");
        jdbc.update("update moments set deleted_at=now() where id=?",shared);
        JsonNode message=json.readTree(call("GET","/api/conversations/"+conversation+"/messages",b,null).body()).get("items").get(0);
        assertThat(message.get("momentId").isNull()).isTrue();assertThat(message.get("content").isNull()).isTrue();
        assertThat(momentSend(a,conversation,shared).statusCode()).isEqualTo(404);
    }
    @Test void rollbackDoesNotPublishAndForeignReadAnchorDoesNotMark() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        jdbc.update("insert into device_tokens(id,user_id,fcm_token,device_id,updated_at) values(?,?,?,?,now())",UUID.randomUUID(),b.id,"synthetic-rollback-device","qa");
        transaction().executeWithoutResult(status->{context.getBean(ChatService.class).send(a.id,conversation,new SendRequest(UUID.randomUUID(),"TEXT","rollback",null));status.setRollbackOnly();});
        assertThat(recorded()).isEmpty();assertThat(jdbc.queryForObject("select count(*) from messages where conversation_id=?",Integer.class,conversation)).isZero();
        assertThat(call("PUT","/api/conversations/"+conversation+"/read",b,json.writeValueAsString(Map.of("messageId",UUID.randomUUID()))).statusCode()).isEqualTo(404);
    }
    @Test void messagePagesAreBoundedAndNewestFirst() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        for(int i=0;i<52;i++) context.getBean(ChatService.class).send(a.id,conversation,new SendRequest(UUID.randomUUID(),"TEXT","message "+i,null));
        var page=json.readTree(call("GET","/api/conversations/"+conversation+"/messages",a,null).body());
        assertThat(page.get("items")).hasSize(50);assertThat(page.get("hasNext").asBoolean()).isTrue();
        assertThat(page.get("items").get(0).get("content").asText()).isEqualTo("message 51");
        var tail=json.readTree(call("GET","/api/conversations/"+conversation+"/messages?page=1",a,null).body());
        assertThat(tail.get("items")).hasSize(2);assertThat(tail.get("hasNext").asBoolean()).isFalse();
    }
    @Test void waitingSendRechecksBlockAfterPairLockInsteadOfUsingOldFriendship() throws Exception {
        var a=account();var b=account();friends(a,b);UUID conversation=open(a,b);
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var block=pool.submit(()->transaction().executeWithoutResult(status->{
                context.getBean(FriendService.class).block(b.id,a.id);locked.countDown();
                try {if(!release.await(10,TimeUnit.SECONDS))throw new AssertionError("release timeout");}
                catch(InterruptedException failure) {Thread.currentThread().interrupt();throw new RuntimeException(failure);}
            }));
            assertThat(locked.await(5,TimeUnit.SECONDS)).isTrue();
            var sending=pool.submit(()->send(a,conversation,UUID.randomUUID(),"must not escape block"));
            assertThatThrownBy(()->sending.get(250,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();block.get(10,TimeUnit.SECONDS);
            assertThat(sending.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(404);
            assertThat(jdbc.queryForObject("select count(*) from messages where conversation_id=?",Integer.class,conversation)).isZero();
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void totalUnreadIncludesConversationsBeyondFirstPage() {
        var a=account();var service=context.getBean(ChatService.class);
        for(int i=0;i<22;i++) {
            var peer=account();friends(a,peer);var conversation=service.open(peer.id,a.id);
            service.send(peer.id,conversation.id(),new SendRequest(UUID.randomUUID(),"TEXT","unread",null));
        }
        var inbox=service.list(a.id,0);
        assertThat(inbox.items()).hasSize(20);assertThat(inbox.hasNext()).isTrue();assertThat(inbox.totalUnreadCount()).isEqualTo(22);
    }
}
