package com.taktak.app.data.remote;

import com.google.gson.Gson;
import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.ProfileRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileRepositoryTest {
    private static class Memory implements SessionStore {
        private Session session;
        public Session load(){return session;} public void save(Session value){session=value;} public void clear(){session=null;}
    }
    private TokenResponse tokens() {
        var pair=new TokenResponse();pair.accessToken="header."+UUID.randomUUID().toString().replace("-","")+".signature";
        pair.refreshToken=UUID.randomUUID().toString().replace("-","")+"abcdefghijk";pair.expiresIn=900;pair.tokenType="Bearer";return pair;
    }
    private ApiClient seeded(String base) throws Exception {
        var client=new ApiClient(base,new Memory());client.sessions.completeLogin(client.sessions.beginLogin(),tokens());return client;
    }
    private String profile() {return "{\"id\":\""+UUID.randomUUID()+"\",\"email\":\"qa@example.test\",\"username\":\"qa\",\"displayName\":\"QA\",\"createdAt\":\"2026-10-18T00:00:00Z\"}";}
    @Test public void profileUpdateRefreshesExpiredAccessAndOnlySendsDisplayName() throws Exception {
        var next=tokens();var rotations=new AtomicInteger();var updates=new AtomicInteger();
        try(var server=new TestHttpServer(request -> {
            if(request.path.equals("/api/auth/refresh")) {rotations.incrementAndGet();return new TestHttpServer.Reply(200,new Gson().toJson(next));}
            if(!("Bearer "+next.accessToken).equals(request.authorization))return new TestHttpServer.Reply(401,"{}");
            var body=new Gson().fromJson(request.body,com.google.gson.JsonObject.class);
            assertEquals(java.util.Set.of("displayName"),body.keySet());assertEquals("Tên mới",body.get("displayName").getAsString());updates.incrementAndGet();
            return new TestHttpServer.Reply(200,profile());
        })) {
            var client=seeded(server.baseUrl());assertNotNull(new ProfileRepository(client).update("  Tên mới  "));
            assertEquals(1,rotations.get());assertEquals(1,updates.get());
        }
    }
    @Test public void invalidNamesAndStaleSessionDoNotSendUpdates() throws Exception {
        var requests=new AtomicInteger();try(var server=new TestHttpServer(request -> {requests.incrementAndGet();return new TestHttpServer.Reply(500,"{}");})) {
            var client=seeded(server.baseUrl());var repository=new ProfileRepository(client);
            for(String name:new String[]{"", " \t ","a".repeat(256)}) {
                try{repository.update(name);fail();}catch(ApiFailure failure){assertEquals(ApiFailure.Kind.VALIDATION,failure.kind);}
            }
            client.sessions.clear();try{repository.load();fail();}catch(ApiFailure failure){assertEquals(ApiFailure.Kind.CANCELLED,failure.kind);}
            assertEquals(0,requests.get());
        }
    }
    @Test public void inFlightProfileCannotPublishAfterAccountSwitch() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var server=new TestHttpServer(request -> {entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return new TestHttpServer.Reply(200,profile());})) {
            var client=seeded(server.baseUrl());var repository=new ProfileRepository(client);var executor=Executors.newSingleThreadExecutor();
            try {
                var call=executor.submit(() -> {try{repository.load();return false;}catch(ApiFailure failure){return failure.kind==ApiFailure.Kind.CANCELLED;}});
                assertTrue(entered.await(5,TimeUnit.SECONDS));client.sessions.completeLogin(client.sessions.beginLogin(),tokens());release.countDown();
                assertTrue(call.get(5,TimeUnit.SECONDS));assertFalse(repository.publishIfCurrent(() -> fail("Old session")));
            } finally {release.countDown();executor.shutdownNow();}
        }
    }
}
