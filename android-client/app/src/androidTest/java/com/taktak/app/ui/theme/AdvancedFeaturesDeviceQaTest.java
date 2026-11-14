package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import androidx.lifecycle.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.*;
import com.taktak.app.data.outbox.*;
import com.taktak.app.ui.camera.*;
import com.taktak.app.data.repository.CaptureDraftRepository;
import com.taktak.app.viewmodel.SessionViewModel;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Acceptance QA: không dùng phiên cá nhân, không gửi ảnh/video hoặc gọi VPS. */
@RunWith(AndroidJUnit4.class)
public class AdvancedFeaturesDeviceQaTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Test public void dualCompositorKeepsSquareAndPipColor()throws Exception{
        Bitmap main=Bitmap.createBitmap(200,160,Bitmap.Config.ARGB_8888),pip=Bitmap.createBitmap(90,120,Bitmap.Config.ARGB_8888);
        main.eraseColor(Color.RED);pip.eraseColor(Color.BLUE);
        try{for(boolean left:new boolean[]{true,false}){
            byte[] data=DualCameraCompositor.composite(main,90,false,pip,270,true,left);
            Bitmap result=BitmapFactory.decodeByteArray(data,0,data.length);assertNotNull(result);
            try{assertEquals(1080,result.getWidth());assertEquals(1080,result.getHeight());
                int color=result.getPixel(left?198:882,198);assertTrue(Color.blue(color)>Color.red(color));assertTrue(data.length<=800*1024);
            }finally{result.recycle();}
        }}finally{main.recycle();pip.recycle();}
    }
    private OutboxRecord record(String id,String path){return new OutboxRecord(id,"qa-owner",987654321L,UUID.randomUUID().toString(),"PHOTO",path,null,"QA",List.of(),null,System.currentTimeMillis(),OutboxStatus.PENDING,0,0,null,0);}
    @Test public void offlineRecordSurvivesDatabaseReopen()throws Exception{
        String id=UUID.randomUUID().toString();var db=new OutboxDatabaseHelper(context);
        try{var first=record(id,"qa-only-no-file");db.insert(first);db.close();db=new OutboxDatabaseHelper(context);
            assertEquals(first.idempotencyKey,db.get(id).idempotencyKey);assertEquals(first.ownerUserId,db.get(id).ownerUserId);
        }finally{db.delete(id);db.close();}
    }
    @Test public void outboxWrongSessionCannotCompleteOrDeletePayload()throws Exception{
        var payload=java.io.File.createTempFile("qa-outbox-",".jpg",context.getCacheDir());java.nio.file.Files.write(payload.toPath(),new byte[]{1,2,3});
        String id=UUID.randomUUID().toString();var db=new OutboxDatabaseHelper(context);
        try{db.insert(record(id,payload.getAbsolutePath()));OutboxRepository.getInstance(context).markCompleted(id,987654322L);
            assertEquals("Wrong generation must not update record",OutboxStatus.PENDING,db.get(id).status);
            assertTrue("Wrong generation must not delete payload",payload.exists());
        }finally{db.delete(id);db.close();java.nio.file.Files.deleteIfExists(payload.toPath());}
    }
    @Test public void cancelledTaskCannotBeRevivedByLateProgress()throws Exception{
        String id=UUID.randomUUID().toString();var db=new OutboxDatabaseHelper(context);
        try{db.insert(record(id,"qa-no-file"));assertTrue(db.updateStatus(id,OutboxStatus.CANCELLED,null,0,987654321L));
            assertFalse(db.updateStatus(id,OutboxStatus.UPLOADING,null,50,987654321L));
            OutboxRepository.getInstance(context).markCompleted(id,987654321L);
            assertEquals(OutboxStatus.CANCELLED,db.get(id).status);
        }finally{db.delete(id);db.close();}
    }
    @Test public void outboxV1UpgradePreservesRowsAndAddsCheckpoint()throws Exception{
        var raw=android.database.sqlite.SQLiteDatabase.create(null);var helper=new OutboxDatabaseHelper(context);
        try{raw.execSQL("CREATE TABLE outbox_tasks(id TEXT PRIMARY KEY,caption TEXT)");
            raw.execSQL("INSERT INTO outbox_tasks VALUES('qa','retained')");helper.onUpgrade(raw,1,2);
            try(var c=raw.rawQuery("SELECT caption,created_moment_id FROM outbox_tasks WHERE id='qa'",null)){
                assertTrue(c.moveToFirst());assertEquals("retained",c.getString(0));assertTrue(c.isNull(1));
            }
        }finally{raw.close();helper.close();}
    }
    @Test public void liveAudioPermissionIsDeclared()throws Exception{
        var info=context.getPackageManager().getPackageInfo(context.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS);
        assertTrue("Live audio permission must be declared before runtime request",Arrays.asList(info.requestedPermissions).contains(android.Manifest.permission.RECORD_AUDIO));
    }
    @Test public void outboxPollFailureMustNotBeReportedAsComplete()throws Exception{
        ChatThemeFixture fixture=new ChatThemeFixture(context);fixture.profileEnabled=true;OutboxRecord task=null;AtomicBoolean pollCalled=new AtomicBoolean();
        AtomicBoolean pollHealthy=new AtomicBoolean();java.util.concurrent.atomic.AtomicInteger uploads=new java.util.concurrent.atomic.AtomicInteger();
        try{
            for(String field:new String[]{"moments","poll"}){
                Class<?> type=field.equals("moments")?com.taktak.app.data.remote.MomentApi.class:com.taktak.app.data.remote.PollApi.class;
                Object api=java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(o,m,a)->{
                    if(field.equals("poll"))pollCalled.set(true);
                    else uploads.incrementAndGet();
                    var created=new com.taktak.app.data.remote.MomentApi.CreatedMoment();created.id="00000000-0000-4000-8000-000000000123";
                    var poll=new com.taktak.app.data.model.PollModels.PollResponse();poll.pollId=UUID.randomUUID().toString();poll.momentId=created.id;poll.question="QA?";poll.optionA="A";poll.optionB="B";poll.status="OPEN";
                    retrofit2.Response<?> response=field.equals("moments")?retrofit2.Response.success(created):pollHealthy.get()?retrofit2.Response.success(poll):retrofit2.Response.error(503,okhttp3.ResponseBody.create("{}",okhttp3.MediaType.get("application/json")));
                    return java.lang.reflect.Proxy.newProxyInstance(retrofit2.Call.class.getClassLoader(),new Class<?>[]{retrofit2.Call.class},(call,method,args)->switch(method.getName()){
                        case "execute"->response;case "isExecuted","isCanceled"->false;case "cancel"->null;
                        case "timeout"->new okio.Timeout();case "request"->new okhttp3.Request.Builder().url("http://127.0.0.1:9/qa").build();default->"QA fixture";
                    });
                });
                var declared=com.taktak.app.data.remote.ApiClient.class.getField(field);declared.setAccessible(true);declared.set(fixture.client,api);
            }
            var repo=OutboxRepository.getInstance(context);
            task=repo.enqueue(0,ChatThemeFixture.SELF,new byte[]{1,2,3},"PHOTO",null,"QA",List.of(),"{\"question\":\"QA?\",\"optionA\":\"A\",\"optionB\":\"B\"}");
            String id=task.id;await(()->{var status=repo.get(id).status;return pollCalled.get()&&status!=OutboxStatus.PENDING&&status!=OutboxStatus.WAITING_NETWORK&&status!=OutboxStatus.UPLOADING;});
            assertEquals("Poll failure must remain retryable, not silently complete",OutboxStatus.FAILED_RETRYABLE,repo.get(id).status);
            assertEquals("00000000-0000-4000-8000-000000000123",repo.createdMomentId(id,0));
            try(var reopened=new OutboxDatabaseHelper(context)){
                assertEquals(repo.createdMomentId(id,0),reopened.createdMomentId(id,0));
            }
            String key=task.idempotencyKey;pollHealthy.set(true);repo.retry(id,0);
            await(()->repo.get(id).status==OutboxStatus.COMPLETED&&!new java.io.File(repo.get(id).payloadPath).exists());
            assertEquals("Retry must not re-upload media",1,uploads.get());assertEquals(key,repo.get(id).idempotencyKey);
        }finally{
            if(task!=null){androidx.work.WorkManager.getInstance(context).cancelUniqueWork("outbox_"+task.id).getResult().get();var db=new OutboxDatabaseHelper(context);db.delete(task.id);db.close();java.nio.file.Files.deleteIfExists(new java.io.File(task.payloadPath).toPath());}
            fixture.close();
        }
    }
    @Test public void captureDualAndLiveIntoPrivateCache()throws Exception{
        var inst=InstrumentationRegistry.getInstrumentation();MainActivity host=null;ChatThemeFixture fixture=null;
        List<Activity> opened=new ArrayList<>();List<String> drafts=new ArrayList<>();
        try{
            // Runner cấp quyền trước suite và khôi phục sau suite: revoke sẽ kill process.
            assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(android.Manifest.permission.CAMERA));
            assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO));
            fixture=new ChatThemeFixture(context);
            try(var launch=inst.getUiAutomation().executeShellCommand("am start -n com.taktak.app/.MainActivity --ez qa true")){try(var in=new java.io.FileInputStream(launch.getFileDescriptor())){in.readAllBytes();}}
            AtomicReference<MainActivity> ref=new AtomicReference<>();await(()->{main(()->{for(Activity a:resumed())if(a instanceof MainActivity m)ref.set(m);});return ref.get()!=null;});host=ref.get();opened.add(host);MainActivity activity=host;
            await(()->{AtomicReference<SessionViewModel.State> s=new AtomicReference<>();main(()->s.set(new ViewModelProvider(activity).get(SessionViewModel.class).state().getValue()));return s.get()!=null&&s.get().destination==SessionViewModel.Destination.ERROR;});
            main(()->((MutableLiveData<SessionViewModel.State>)new ViewModelProvider(activity).get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
            await(()->enabled(activity,R.id.captureButton));
            main(()->activity.findViewById(R.id.dualModeButton).performClick());screenshot("advanced4-dual-controls.png");
            holdShutter(activity,60,false);
            PreviewActivity preview=preview();opened.add(preview);String id=preview.getIntent().getStringExtra(PreviewActivity.DRAFT_ID);drafts.add(id);
            var store=new CaptureDraftRepository(context,0);Bitmap image=BitmapFactory.decodeFile(store.file(id).getAbsolutePath());assertNotNull(image);
            try{assertEquals(image.getWidth(),image.getHeight());}finally{image.recycle();}
            screenshot("advanced4-dual-preview.png");main(preview::finish);
            // Main re-checks session on resume; inject QA gate again, never real profile/FCM.
            Thread.sleep(500);main(()->((MutableLiveData<SessionViewModel.State>)new ViewModelProvider(activity).get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
            await(()->enabled(activity,R.id.captureButton));
            // Cancel không được biến thành một lần chụp ảnh hoặc mở Preview.
            holdShutter(activity,1800,true);
            await(()->enabled(activity,R.id.captureButton));
            AtomicBoolean extraPreview=new AtomicBoolean();main(()->extraPreview.set(resumed().stream().anyMatch(a->a instanceof PreviewActivity)));
            assertFalse("Cancelled hold must not open Preview",extraPreview.get());
            holdShutter(activity,4500,false);PreviewActivity live=preview();opened.add(live);
            String liveId=live.getIntent().getStringExtra(PreviewActivity.DRAFT_ID);drafts.add(liveId);
            var retriever=new android.media.MediaMetadataRetriever();try{retriever.setDataSource(store.videoFile(liveId).getAbsolutePath());long ms=Long.parseLong(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION));
                assertTrue("Live recording must fit server tolerance; actual="+ms,ms>=2000&&ms<=3300);
            }finally{retriever.release();}
            assertFalse(com.taktak.app.data.repository.LiveVideoMetadata.read(store.videoFile(liveId)).hasAudio());
            screenshot("advanced4-live-preview.png");
            main(live::finish);Thread.sleep(500);
            main(()->((MutableLiveData<SessionViewModel.State>)new ViewModelProvider(activity).get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
            await(()->enabled(activity,R.id.btnLiveAudioToggle));main(()->activity.findViewById(R.id.btnLiveAudioToggle).performClick());
            await(()->enabled(activity,R.id.captureButton));holdShutter(activity,1800,false);
            PreviewActivity audio=preview();opened.add(audio);String audioId=audio.getIntent().getStringExtra(PreviewActivity.DRAFT_ID);drafts.add(audioId);
            var metadata=com.taktak.app.data.repository.LiveVideoMetadata.read(store.videoFile(audioId));
            assertTrue("Mic-enabled clip must contain an audio track",metadata.hasAudio());
            assertTrue("Release must stop before 3s cap",metadata.durationMs()>0&&metadata.durationMs()<2500);
        }finally{
            main(()->{for(int i=opened.size()-1;i>=0;i--)opened.get(i).finish();});
            for(String id:drafts){try{new CaptureDraftRepository(context,0).discard(id);}catch(Exception ignored){}}
            if(fixture!=null)fixture.close();
        }
    }
    private void holdShutter(Activity activity,long millis,boolean cancel)throws Exception{
        AtomicReference<android.view.View> ref=new AtomicReference<>();
        long down=android.os.SystemClock.uptimeMillis();
        main(()->{
            var view=activity.findViewById(R.id.captureButton);ref.set(view);
            var event=android.view.MotionEvent.obtain(down,down,android.view.MotionEvent.ACTION_DOWN,view.getWidth()/2f,view.getHeight()/2f,0);
            try{view.dispatchTouchEvent(event);}finally{event.recycle();}
        });
        Thread.sleep(millis);
        main(()->{
            var view=ref.get();
            var event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),
                    cancel?android.view.MotionEvent.ACTION_CANCEL:android.view.MotionEvent.ACTION_UP,view.getWidth()/2f,view.getHeight()/2f,0);
            try{view.dispatchTouchEvent(event);}finally{event.recycle();}
        });
    }
    private List<Activity> resumed(){return new ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED));}
    private PreviewActivity preview()throws Exception{
        AtomicReference<PreviewActivity> ref=new AtomicReference<>();
        await(()->{main(()->{for(Activity a:resumed())if(a instanceof PreviewActivity p)ref.set(p);});return ref.get()!=null;});
        PreviewActivity activity=ref.get();
        await(()->{AtomicBoolean ready=new AtomicBoolean();main(()->{
            var state=new ViewModelProvider(activity).get(SessionViewModel.class).state().getValue();
            ready.set(state!=null&&state.destination==SessionViewModel.Destination.ERROR);
        });return ready.get();});
        main(()->((MutableLiveData<SessionViewModel.State>)new ViewModelProvider(activity).get(SessionViewModel.class).state())
                .setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
        await(()->{AtomicBoolean ready=new AtomicBoolean();main(()->{
            var image=(android.widget.ImageView)activity.findViewById(R.id.previewImage);
            ready.set(image.isShown()&&image.getDrawable()!=null);
        });return ready.get();});
        Thread.sleep(400);
        return activity;
    }
    private boolean enabled(Activity activity,int id){AtomicBoolean r=new AtomicBoolean();main(()->{var v=activity.findViewById(id);r.set(v!=null&&v.isShown()&&v.isEnabled());});return r.get();}
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void await(BooleanSupplier condition)throws Exception{long end=System.currentTimeMillis()+30000;while(System.currentTimeMillis()<end){if(condition.getAsBoolean())return;Thread.sleep(100);}throw new AssertionError("QA UI condition timeout");}
    private void screenshot(String name)throws Exception{var inst=InstrumentationRegistry.getInstrumentation();inst.waitForIdleSync();var bmp=inst.getUiAutomation().takeScreenshot();assertNotNull(bmp);var dir=new java.io.File(context.getExternalFilesDir(null),"artifacts");dir.mkdirs();try(var out=new java.io.FileOutputStream(new java.io.File(dir,name))){bmp.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bmp.recycle();}}
}
