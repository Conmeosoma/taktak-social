package com.taktak.app;

import android.content.*;
import android.graphics.*;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.ui.history.*;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PrivacyRegressionAcceptanceTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AuthRepository auth;
    private boolean owned;
    private long generation;
    @Before public void fixture() throws Exception {
        if(android.os.Build.VERSION.SDK_INT>=37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),"android.permission.ACCESS_LOCAL_NETWORK");
        }
        auth=AuthGraph.repository(context);
        assertFalse("Không thay phiên cá nhân khi QA",auth.hasSession());
        String name="privacy_"+UUID.randomUUID().toString().replace("-", ""),password=UUID.randomUUID().toString();
        auth.register(name+"@example.test",name,"Privacy QA",password); auth.login(name+"@example.test",password); owned=true;
        generation = auth.sessionGeneration();
    }
    @After public void cleanup() throws Exception { if(owned) auth.logout(context); HistoryImageCache.clear(); }
    private String upload() throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.MAGENTA);
        var bytes=new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes); bitmap.recycle();
        return auth.moments().upload(bytes.toByteArray());
    }
    private Intent viewer(String id) {
        return new Intent(context,MomentViewerActivity.class).putExtra(MomentViewerActivity.EXTRA_MOMENT_ID,id)
                .putExtra(MomentViewerActivity.EXTRA_SENDER_NAME,"Private identity")
                .putExtra(MomentViewerActivity.EXTRA_CAPTION,"Private caption");
    }
    private void await(ActivityScenario<MomentViewerActivity> scenario,int resource) {
        long deadline=android.os.SystemClock.elapsedRealtime()+30000;
        while(android.os.SystemClock.elapsedRealtime()<deadline) {
            AtomicBoolean found=new AtomicBoolean(); scenario.onActivity(activity -> found.set(activity.findViewById(resource).isShown()));
            if(found.get()) return; android.os.SystemClock.sleep(100);
        }
        fail("Viewer state not reached: "+resource);
    }
    private void capture(String name) throws Exception {
        var directory=new java.io.File(context.getExternalFilesDir(null),"qa-review-fixes");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try(var output=new java.io.FileOutputStream(new java.io.File(directory,name+".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,output));
        } finally { screenshot.recycle(); }
    }
    @Test public void revokedServerImageIsNotRestoredFromCacheOrSaved() throws Exception {
        String id=upload(); byte[] bytes=auth.moments().download(id);
        HistoryImageCache.rememberAuthorized(id,generation,BitmapFactory.decodeByteArray(bytes,0,bytes.length)); auth.moments().delete(id);
        try(var scenario=ActivityScenario.<MomentViewerActivity>launch(viewer(id))) {
            await(scenario,R.id.viewerErrorView);
            capture("revoked-viewer");
            scenario.onActivity(activity -> {
                assertNull(((android.widget.ImageView)activity.findViewById(R.id.viewerImage)).getDrawable());
                assertFalse(activity.findViewById(R.id.viewerSaveButton).isEnabled());
                assertFalse(activity.findViewById(R.id.viewerSenderName).isShown());
            });
            assertNull(HistoryImageCache.getAuthorized(id,generation));
        }
    }
    @Test public void forbiddenImageDoesNotUseAnotherAccountsCachedBitmap() throws Exception {
        var store=new com.taktak.app.data.session.SessionStore() {
            private com.taktak.app.data.session.Session value;
            public com.taktak.app.data.session.Session load() { return value; }
            public void save(com.taktak.app.data.session.Session next) { value=next; }
            public void clear() { value=null; }
        };
        var other=new AuthRepository(new com.taktak.app.data.remote.ApiClient(BuildConfig.BASE_URL,store));
        String name="denied_"+UUID.randomUUID().toString().replace("-",""), password=UUID.randomUUID().toString();
        other.register(name+"@example.test",name,"Other QA",password); other.login(name+"@example.test",password);
        try {
            Bitmap bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.CYAN);
            var bytes=new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes); bitmap.recycle();
            String id=other.moments().upload(bytes.toByteArray());
            HistoryImageCache.rememberAuthorized(id,generation,BitmapFactory.decodeByteArray(bytes.toByteArray(),0,bytes.size()));
            try(var scenario=ActivityScenario.<MomentViewerActivity>launch(viewer(id))) {
                await(scenario,R.id.viewerErrorView);
                scenario.onActivity(activity -> assertFalse(activity.findViewById(R.id.viewerSaveButton).isEnabled()));
                assertNull(HistoryImageCache.getAuthorized(id,generation));
            }
        } finally { other.logout(); }
    }
    @Test public void saveRevalidatesPermissionInsteadOfExportingCachedDeletedImage() throws Exception {
        String id=upload();
        try(var scenario=ActivityScenario.<MomentViewerActivity>launch(viewer(id))) {
            long deadline=android.os.SystemClock.elapsedRealtime()+30000; AtomicBoolean loaded=new AtomicBoolean();
            while(!loaded.get() && android.os.SystemClock.elapsedRealtime()<deadline) {
                scenario.onActivity(activity -> loaded.set(activity.findViewById(R.id.viewerSaveButton).isEnabled()));
                android.os.SystemClock.sleep(100);
            }
            assertTrue(loaded.get()); assertNotNull(HistoryImageCache.getAuthorized(id,generation));
            int before=galleryCount(); auth.moments().delete(id);
            // Không gửi push local: thao tác Save phải tự kiểm tra server lần nữa.
            scenario.onActivity(activity -> activity.findViewById(R.id.viewerSaveButton).performClick());
            await(scenario,R.id.viewerErrorView);
            assertEquals(before,galleryCount()); assertNull(HistoryImageCache.getAuthorized(id,generation));
            capture("save-denied-no-cache-fallback");
        }
    }
    private int galleryCount() {
        try(var cursor=context.getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{android.provider.MediaStore.Images.Media._ID},
                android.provider.MediaStore.Images.Media.DISPLAY_NAME+" LIKE ?",new String[]{"taktak_moment%"},null)) {
            assertNotNull(cursor); return cursor.getCount();
        }
    }
    @Test public void deletionEventInvalidatesOpenViewerImmediately() throws Exception {
        String id=upload();
        try(var scenario=ActivityScenario.<MomentViewerActivity>launch(viewer(id))) {
            long deadline=android.os.SystemClock.elapsedRealtime()+30000; AtomicBoolean loaded=new AtomicBoolean();
            while(!loaded.get() && android.os.SystemClock.elapsedRealtime()<deadline) {
                scenario.onActivity(activity -> loaded.set(activity.findViewById(R.id.viewerSaveButton).isEnabled()));
                android.os.SystemClock.sleep(100);
            }
            assertTrue(loaded.get()); auth.moments().delete(id); PushEventBus.emitMomentDeleted(id);
            await(scenario,R.id.viewerErrorView);
            scenario.onActivity(activity -> assertFalse(activity.findViewById(R.id.viewerSaveButton).isEnabled()));
            assertNull(HistoryImageCache.getAuthorized(id,generation));
        }
    }
    @Test public void logoutClearsCacheAndRejectsOldWidgetPublication() throws Exception {
        var old=auth.moments(); String id=upload();
        HistoryImageCache.rememberAuthorized(id,generation,Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888));
        auth.logout(context); owned=false;
        assertNull(HistoryImageCache.getAuthorized(id,generation));
        AtomicBoolean rendered=new AtomicBoolean(); assertFalse(old.publishIfCurrent(() -> rendered.set(true)));
        assertFalse(rendered.get());
    }
    @Test public void logoutHidesImageInAlreadyOpenViewer() throws Exception {
        String id=upload();
        try(var scenario=ActivityScenario.<MomentViewerActivity>launch(viewer(id))) {
            long deadline=android.os.SystemClock.elapsedRealtime()+30000; AtomicBoolean loaded=new AtomicBoolean();
            while(!loaded.get() && android.os.SystemClock.elapsedRealtime()<deadline) {
                scenario.onActivity(activity -> loaded.set(activity.findViewById(R.id.viewerSaveButton).isEnabled()));
                android.os.SystemClock.sleep(100);
            }
            assertTrue(loaded.get()); auth.logout(context); owned=false;
            await(scenario,R.id.viewerErrorView);
            scenario.onActivity(activity -> {
                assertNull(((android.widget.ImageView)activity.findViewById(R.id.viewerImage)).getDrawable());
                assertFalse(activity.findViewById(R.id.viewerSaveButton).isEnabled());
                assertFalse(activity.findViewById(R.id.viewerSenderName).isShown());
            });
        }
    }
}
