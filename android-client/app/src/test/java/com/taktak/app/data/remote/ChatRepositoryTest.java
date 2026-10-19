package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.model.ChatModels.Send;
import com.taktak.app.data.repository.ChatRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contract/phiên dùng HTTP loopback test, không account hay backend thật. */
public class ChatRepositoryTest {
    private static final String CHAT="00000000-0000-4000-8000-000000000001";
    private static final String SELF="00000000-0000-4000-8000-000000000002";
    private static final String PEER="00000000-0000-4000-8000-000000000003";
    private static final String MESSAGE="00000000-0000-4000-8000-000000000004";
    private static final String KEY="00000000-0000-4000-8000-000000000005";
    private static final String TEXT="{\"id\":\""+MESSAGE+"\",\"conversationId\":\""+CHAT+"\",\"senderId\":\""+SELF+"\",\"recipientId\":\""+PEER+"\",\"clientMessageId\":\""+KEY+"\",\"messageType\":\"TEXT\",\"content\":\"hello\",\"createdAt\":\"2026-10-20T00:00:00Z\"}";
    private static final String ROOM="{\"id\":\""+CHAT+"\",\"peer\":{\"id\":\""+PEER+"\",\"username\":\"qa_peer\",\"displayName\":\"QA Peer\"},\"updatedAt\":\"2026-10-20T00:00:00Z\",\"unreadCount\":1,\"lastMessage\":"+TEXT+"}";
    private static class Memory implements SessionStore {
        Session value;
        public Session load(){return value;}
        public void save(Session next){value=next;}
        public void clear(){value=null;}
    }
    private static void seed(ApiClient client) throws Exception {
        var tokens=new TokenResponse(); tokens.accessToken="qa."+UUID.randomUUID()+".signature";
        tokens.refreshToken=UUID.randomUUID().toString().replace("-","")+"abcdefghijk";
        tokens.expiresIn=900; tokens.tokenType="Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(),tokens);
    }
    private static void fails(ApiFailure.Kind kind,Callable<?> action) throws Exception {
        try {action.call(); fail("Expected typed failure");}
        catch(ApiFailure error){assertEquals(kind,error.kind);}
    }
    @Test public void protectedConversationAndPagesMatchContract() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try(var server=new TestHttpServer(request->{
            assertTrue(request.authorization.startsWith("Bearer ")); calls.incrementAndGet();
            if(request.path.endsWith("messages?page=0"))return new TestHttpServer.Reply(200,"{\"items\":["+TEXT+"],\"hasNext\":true}");
            if(request.path.equals("/api/conversations?page=0"))return new TestHttpServer.Reply(200,"{\"items\":["+ROOM+"],\"hasNext\":false}");
            return new TestHttpServer.Reply(200,ROOM);
        })){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);var repo=new ChatRepository(client);
            assertEquals(CHAT,repo.inbox(0).items.get(0).id);
            assertEquals(PEER,repo.withFriend(PEER).peer.id);
            assertEquals(CHAT,repo.conversation(CHAT).id);
            assertTrue(repo.messages(CHAT,0).hasNext);assertEquals(4,calls.get());
        }
    }
    @Test public void retryUsesSameExplicitIdempotencyKeyAndReadAccepts204() throws Exception {
        AtomicInteger sends=new AtomicInteger();
        try(var server=new TestHttpServer(request->{
            if(request.path.endsWith("/read")){
                assertEquals("{\"messageId\":\""+MESSAGE+"\"}",request.body);
                return new TestHttpServer.Reply(204,"");
            }
            assertEquals("{\"clientMessageId\":\""+KEY+"\",\"messageType\":\"TEXT\",\"content\":\"hello\"}",request.body);
            return sends.incrementAndGet()==1?new TestHttpServer.Reply(503,"{}"):new TestHttpServer.Reply(200,TEXT);
        })){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);var repo=new ChatRepository(client);
            var payload=new Send(KEY,"TEXT","hello",null);
            fails(ApiFailure.Kind.SERVER,()->repo.send(CHAT,payload));assertEquals(1,sends.get());
            assertEquals(KEY,repo.send(CHAT,payload).clientMessageId);assertEquals(2,sends.get());repo.read(CHAT,MESSAGE);
        }
    }
    @Test public void invalidInputsAreRejectedBeforeTransport() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try(var server=new TestHttpServer(request->{calls.incrementAndGet();return new TestHttpServer.Reply(200,"{}");})){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);var repo=new ChatRepository(client);
            fails(ApiFailure.Kind.VALIDATION,()->repo.inbox(-1));
            fails(ApiFailure.Kind.VALIDATION,()->repo.messages(CHAT,10001));
            fails(ApiFailure.Kind.VALIDATION,()->repo.withFriend("../users"));
            for(Send payload:new Send[]{null,new Send(KEY,"TEXT"," ",null),new Send(KEY,"TEXT","a\0b",null),new Send(KEY,"TEXT","a".repeat(2001),null),new Send(KEY,"TEXT","hello",MESSAGE),new Send(KEY,"MOMENT","text",MESSAGE),new Send(KEY,"OTHER",null,null)}){
                fails(ApiFailure.Kind.VALIDATION,()->repo.send(CHAT,payload));
            }
            assertEquals(0,calls.get());
        }
    }
    @Test public void unicodeLimitCountsCodePointsNotUtf16Units(){
        assertTrue(ChatRepository.validText("😀".repeat(2000)));
        assertFalse(ChatRepository.validText("😀".repeat(2001)));
        assertFalse(ChatRepository.validText("\u2003 \n"));assertFalse(ChatRepository.validText(null));
    }
    @Test public void successfulButForeignBodiesCannotEscape() throws Exception {
        for(String body:new String[]{TEXT.replace(CHAT,PEER),TEXT.replace(KEY,PEER),TEXT.replace("TEXT","UNKNOWN"),"null"}){
            try(var server=new TestHttpServer(request->new TestHttpServer.Reply(200,body))){
                var client=new ApiClient(server.baseUrl(),new Memory());seed(client);
                fails(ApiFailure.Kind.SERVER,()->new ChatRepository(client).send(CHAT,new Send(KEY,"TEXT","hello",null)));
            }
        }
    }
    @Test public void malformedPagesAndReadResponseAreNotSuccess() throws Exception {
        for(String body:new String[]{"{\"items\":[null]}","{\"items\":null}","{\"items\":["+TEXT.replace(CHAT,PEER)+"]}"}){
            try(var server=new TestHttpServer(request->new TestHttpServer.Reply(200,body))){
                var client=new ApiClient(server.baseUrl(),new Memory());seed(client);
                fails(ApiFailure.Kind.SERVER,()->new ChatRepository(client).messages(CHAT,0));
            }
        }
        try(var server=new TestHttpServer(request->new TestHttpServer.Reply(200,"{}"))){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);
            fails(ApiFailure.Kind.SERVER,()->{new ChatRepository(client).read(CHAT,MESSAGE);return null;});
        }
    }
    @Test public void staleSessionAndPublicationAreRejectedWithoutNetwork() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try(var server=new TestHttpServer(request->{calls.incrementAndGet();return new TestHttpServer.Reply(200,ROOM);})){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);var repo=new ChatRepository(client);seed(client);
            fails(ApiFailure.Kind.CANCELLED,()->repo.withFriend(PEER));
            assertFalse(repo.publishIfCurrent(()->fail("Old publication")));
            client.sessions.beginLogin();fails(ApiFailure.Kind.UNAUTHORIZED,()->repo.conversation(CHAT));assertEquals(0,calls.get());
        }
    }
    @Test public void lateAccountResponseIsNotPublished() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        try(var server=new TestHttpServer(request->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return new TestHttpServer.Reply(200,ROOM);})){
            var client=new ApiClient(server.baseUrl(),new Memory());seed(client);var repo=new ChatRepository(client);
            var future=pool.submit(()->repo.conversation(CHAT));assertTrue(entered.await(5,TimeUnit.SECONDS));seed(client);release.countDown();
            try{future.get(5,TimeUnit.SECONDS);fail("Stale result escaped");}
            catch(ExecutionException error){assertEquals(ApiFailure.Kind.CANCELLED,((ApiFailure)error.getCause()).kind);}
        }finally{release.countDown();pool.shutdownNow();}
    }
}
