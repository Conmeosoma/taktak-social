package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.repository.MomentRepository;
import com.taktak.app.data.session.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionPublishGuardTest {
    private static class Memory implements SessionStore {
        Session value;
        public Session load() { return value; }
        public void save(Session value) { this.value=value; }
        public void clear() { value=null; }
    }
    private ApiClient client() throws Exception {
        var client=new ApiClient("http://127.0.0.1:1/",new Memory()); login(client); return client;
    }
    private void login(ApiClient client) throws Exception {
        var pair=new TokenResponse(); pair.accessToken="header."+UUID.randomUUID()+".signature";
        pair.refreshToken=UUID.randomUUID().toString().replace("-", "")+"abcdefghijk"; pair.expiresIn=900; pair.tokenType="Bearer";
        client.sessions.completeLogin(client.sessions.beginLogin(),pair);
    }
    @Test public void staleRepositoryCannotPublishAfterLogoutOrNewLogin() throws Exception {
        var client=client(); var old=new MomentRepository(client); AtomicInteger published=new AtomicInteger();
        assertTrue(old.publishIfCurrent(published::incrementAndGet));
        client.sessions.clear(); assertFalse(old.publishIfCurrent(published::incrementAndGet));
        login(client); assertFalse(old.publishIfCurrent(published::incrementAndGet));
        assertTrue(new MomentRepository(client).publishIfCurrent(published::incrementAndGet));
        assertEquals(2,published.get());
    }
    @Test public void staleLoggedOutWorkerCannotOverwriteNewSession() throws Exception {
        var client=client(); AtomicBoolean shown=new AtomicBoolean();
        assertFalse(client.sessions.runIfLoggedOut(() -> shown.set(true)));
        client.sessions.clear(); assertTrue(client.sessions.runIfLoggedOut(() -> shown.set(true)));
        shown.set(false); login(client);
        assertFalse(client.sessions.runIfLoggedOut(() -> shown.set(true))); assertFalse(shown.get());
    }
    @Test public void logoutRenderIsOrderedAfterInFlightPublish() throws Exception {
        var client=client(); long generation=client.sessions.snapshot().generation;
        var entered=new CountDownLatch(1); var release=new CountDownLatch(1); AtomicInteger rendered=new AtomicInteger();
        client.sessions.setOnCleared(() -> rendered.set(2));
        var pool=Executors.newFixedThreadPool(2);
        try {
            var publish=pool.submit(() -> client.sessions.runIfCurrent(generation,() -> {
                entered.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS)) throw new AssertionError("Release renderer"); }
                catch(InterruptedException error) { throw new AssertionError(error); }
                rendered.set(1);
            }));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            var clear=pool.submit(() -> { client.sessions.clear(); return true; });
            assertFalse(clear.isDone()); release.countDown();
            assertTrue(publish.get(5,TimeUnit.SECONDS)); assertTrue(clear.get(5,TimeUnit.SECONDS));
            assertEquals(2,rendered.get());
            assertFalse(client.sessions.runIfCurrent(generation,() -> rendered.set(3)));
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
