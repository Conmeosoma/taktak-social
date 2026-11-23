package com.taktak.app.ui.theme;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkManager;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.outbox.*;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Backend/DB QA loopback; runner điều khiển offline/online qua console của LocalSmokeServer. */
@RunWith(AndroidJUnit4.class)
public class OutboxNetworkAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout=org.junit.rules.Timeout.seconds(180);
    private void phase(String value){android.util.Log.i("TaktakOutboxQA",value);}
    private void await(BooleanSupplier condition,String message)throws Exception {
        long end=android.os.SystemClock.elapsedRealtime()+60000;
        while(android.os.SystemClock.elapsedRealtime()<end){if(condition.getAsBoolean())return;Thread.sleep(150);}
        fail(message);
    }
    private boolean online(String base) {
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(base+"actuator/health").openConnection();
            connection.setConnectTimeout(1000);connection.setReadTimeout(1000);
            return connection.getResponseCode()==200;
        } catch(IOException unavailable){return false;}
        finally{if(connection!=null)connection.disconnect();}
    }
    @Test public void offlineQueueRetriesOnlineWithoutDuplicateMedia()throws Exception {
        String base=InstrumentationRegistry.getArguments().getString("taktak.outboxNetworkBaseUrl");
        assertNotNull(base);assertTrue(base.matches("http://127\\.0\\.0\\.1:[1-9][0-9]{3,4}/"));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        var client=new ApiClient(base,new SessionStore(){
            private Session value;
            public Session load(){return value;}public void save(Session next){value=next;}public void clear(){value=null;}
        });
        var auth=new AuthRepository(client);
        var graph=AuthGraph.class.getDeclaredField("repository");graph.setAccessible(true);Object previous=graph.get(null);
        OutboxRecord task=null;String moment=null;
        try {
            String name="qa_outbox_"+UUID.randomUUID().toString().replace("-","");
            String password="Qa!"+UUID.randomUUID();
            auth.register(name+"@example.test",name,"QA Outbox",password);
            var profile=auth.login(name+"@example.test",password);graph.set(null,auth);
            long generation=auth.sessionGeneration();
            phase("READY_TO_PAUSE");await(()->!online(base),"Runner must pause QA backend");
            var bitmap=Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.MAGENTA);
            var bytes=new ByteArrayOutputStream();try{bitmap.compress(Bitmap.CompressFormat.JPEG,85,bytes);}finally{bitmap.recycle();}
            var repo=OutboxRepository.getInstance(context);
            task=repo.enqueue(generation,profile.id,bytes.toByteArray(),"PHOTO",null,"QA offline",List.of(),null);
            String taskId=task.id;
            await(()->{
                try{return WorkManager.getInstance(context).getWorkInfosForUniqueWork("outbox_"+taskId).get()
                        .stream().anyMatch(w->w.getRunAttemptCount()>0);}
                catch(Exception e){throw new AssertionError(e);}
            },"Real worker must attempt and retry while backend is offline");
            assertNotEquals(OutboxStatus.COMPLETED,repo.get(taskId).status);
            assertTrue(new File(task.payloadPath).isFile());
            phase("READY_TO_RESUME");await(()->online(base),"Runner must restore QA backend");
            await(()->repo.get(taskId).status==OutboxStatus.COMPLETED,"Persisted queue must complete after reconnect");
            var page=auth.moments().list("sent",0);assertEquals(1,page.items.size());moment=page.items.get(0).id;
            assertEquals(task.idempotencyKey,repo.get(taskId).idempotencyKey);
            assertEquals(taskId,repo.enqueue(generation,profile.id,bytes.toByteArray(),"PHOTO",null,"QA offline",List.of(),null,task.idempotencyKey).id);
            assertEquals(1,auth.moments().list("sent",0).items.size());
            await(()->!new File(repo.get(taskId).payloadPath).exists(),"Completed payload must be removed");
            phase("OFFLINE_ONLINE_IDEMPOTENT_PASS");
        } finally {
            if(task!=null){WorkManager.getInstance(context).cancelUniqueWork("outbox_"+task.id).getResult().get();
                try(var db=new OutboxDatabaseHelper(context)){db.delete(task.id);}java.nio.file.Files.deleteIfExists(new File(task.payloadPath).toPath());}
            try{if(moment!=null)auth.moments().delete(moment);if(auth.hasSession())auth.logout();}
            finally{graph.set(null,previous);}
        }
    }
}
