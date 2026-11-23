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
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android WorkManager + API/DB/FFmpeg thật, chỉ dữ liệu tổng hợp và account QA mới. */
@RunWith(AndroidJUnit4.class)
public class OutboxMediaApiAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout=org.junit.rules.Timeout.seconds(180);
    @Test public void liveBytesAndPhotoPollSurviveRealWorkerUploadAndDownload()throws Exception {
        var inst=InstrumentationRegistry.getInstrumentation();Context context=inst.getTargetContext();
        String base=InstrumentationRegistry.getArguments().getString("taktak.mediaApiBaseUrl");
        assertNotNull(base);assertTrue(base.matches("http://127\\.0\\.0\\.1:[1-9][0-9]{3,4}/"));
        var client=new ApiClient(base,new SessionStore(){private Session value;
            public Session load(){return value;}public void save(Session next){value=next;}public void clear(){value=null;}});
        var auth=new AuthRepository(client);
        var graph=AuthGraph.class.getDeclaredField("repository");graph.setAccessible(true);Object previous=graph.get(null);
        var records=new ArrayList<OutboxRecord>();var ownedMoments=new ArrayList<String>();
        File thumbnail=File.createTempFile("qa-live-thumb-",".jpg",context.getCacheDir());
        File download=File.createTempFile("qa-live-download-",".mp4",context.getCacheDir());
        try {
            String name="qa_media_"+UUID.randomUUID().toString().replace("-","");String password="Qa!"+UUID.randomUUID();
            auth.register(name+"@example.test",name,"QA Media",password);
            var profile=auth.login(name+"@example.test",password);graph.set(null,auth);long gen=auth.sessionGeneration();
            var bitmap=Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.CYAN);
            var image=new ByteArrayOutputStream();try{bitmap.compress(Bitmap.CompressFormat.JPEG,85,image);}finally{bitmap.recycle();}
            java.nio.file.Files.write(thumbnail.toPath(),image.toByteArray());
            byte[] video;try(var input=inst.getContext().getAssets().open("live-video/silent-3s.mp4")){video=input.readAllBytes();}
            var repo=OutboxRepository.getInstance(context);
            records.add(repo.enqueue(gen,profile.id,video,"LIVE_VIDEO",thumbnail.getAbsolutePath(),"QA Live",List.of(),null));
            java.nio.file.Files.delete(thumbnail.toPath());
            records.add(repo.enqueue(gen,profile.id,image.toByteArray(),"PHOTO",null,"QA Photo Poll",List.of(),
                    "{\"question\":\"QA?\",\"optionA\":\"A\",\"optionB\":\"B\"}"));
            for(var record:records){
                long until=android.os.SystemClock.elapsedRealtime()+60000;
                while(repo.get(record.id).status!=OutboxStatus.COMPLETED&&android.os.SystemClock.elapsedRealtime()<until){
                    assertNotEquals("Worker must not fail permanently",OutboxStatus.FAILED_PERMANENT,repo.get(record.id).status);Thread.sleep(150);
                }
                assertEquals(OutboxStatus.COMPLETED,repo.get(record.id).status);
                ownedMoments.add(repo.createdMomentId(record.id,gen));
            }
            var items=auth.moments().list("sent",0).items;assertEquals(2,items.size());
            String liveId=ownedMoments.get(0),photoId=ownedMoments.get(1);
            auth.moments().downloadVideo(liveId,download);assertArrayEquals(video,java.nio.file.Files.readAllBytes(download.toPath()));
            var live=items.stream().filter(m->m.id.equals(liveId)).findFirst().orElseThrow();
            assertEquals("LIVE_VIDEO",live.mediaType);assertEquals(Integer.valueOf(3000),live.durationMs);assertEquals(Boolean.FALSE,live.hasAudio);
            assertEquals("QA?",auth.polls().getPoll(photoId).question);assertEquals("CLOSED",auth.polls().close(photoId).status);
            for(var record:records){
                long until=android.os.SystemClock.elapsedRealtime()+5000;
                while(new File(record.payloadPath).exists()&&android.os.SystemClock.elapsedRealtime()<until)Thread.sleep(100);
                assertFalse(new File(record.payloadPath).exists());
                if(record.thumbnailPath!=null)assertFalse(new File(record.thumbnailPath).exists());
            }
        } finally {
            for(var record:records){WorkManager.getInstance(context).cancelUniqueWork("outbox_"+record.id).getResult().get();
                try(var db=new OutboxDatabaseHelper(context)){String mid=db.createdMomentId(record.id,record.sessionGeneration);
                    if(mid!=null&&!ownedMoments.contains(mid))ownedMoments.add(mid);db.delete(record.id);}
                java.nio.file.Files.deleteIfExists(new File(record.payloadPath).toPath());
                if(record.thumbnailPath!=null)java.nio.file.Files.deleteIfExists(new File(record.thumbnailPath).toPath());}
            try{for(String id:ownedMoments)if(id!=null)auth.moments().delete(id);if(auth.hasSession())auth.logout();}
            finally{graph.set(null,previous);java.nio.file.Files.deleteIfExists(thumbnail.toPath());java.nio.file.Files.deleteIfExists(download.toPath());}
        }
    }
}
