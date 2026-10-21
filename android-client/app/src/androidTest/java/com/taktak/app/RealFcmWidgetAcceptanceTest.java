package com.taktak.app;

import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import com.taktak.app.widget.TaktakAppWidgetProvider;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Integration opt-in: Firebase thật + backend smoke bật FCM, không giả event/Worker. */
@RunWith(AndroidJUnit4.class)
public class RealFcmWidgetAcceptanceTest {
    @org.junit.Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(300);
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private void phase(String value) {
        // Chỉ tên bước hữu hạn, không log token, ID, tài khoản hoặc payload.
        android.util.Log.i("TaktakOpsQA", "phase=" + value);
    }
    private static class Memory implements SessionStore {
        private Session value;
        public Session load(){return value;}public void save(Session next){value=next;}public void clear(){value=null;}
    }
    private UserProfile account(AuthRepository auth,String label) throws Exception {
        String name="fcmqa_"+UUID.randomUUID().toString().replace("-",""),password=UUID.randomUUID().toString();
        auth.register(name+"@example.test",name,label,password);return auth.login(name+"@example.test",password);
    }
    private byte[] image() {
        var bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.rgb(250,100,140));
        var output=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,output);bitmap.recycle();return output.toByteArray();
    }
    private void await(BooleanSupplier condition,String label) {
        long deadline=SystemClock.elapsedRealtime()+90000;
        while(SystemClock.elapsedRealtime()<deadline) { if(condition.getAsBoolean())return;SystemClock.sleep(200); }
        fail(label);
    }
    private boolean visible(AppWidgetHostView view,int id) {
        var result=new AtomicBoolean();InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var target=view.findViewById(id);result.set(target!=null && target.isShown());
        });return result.get();
    }
    private void captureWidget(AppWidgetHostView widget,String name) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var directory=new java.io.File(context.getExternalFilesDir(null),"qa-fcm-widget");
            assertTrue(directory.isDirectory() || directory.mkdirs());
            var bitmap=Bitmap.createBitmap(widget.getWidth(),widget.getHeight(),Bitmap.Config.ARGB_8888);
            try(var output=new java.io.FileOutputStream(new java.io.File(directory,name+".png"))) {
                widget.draw(new Canvas(bitmap));assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));
            } catch(java.io.IOException failure) {throw new AssertionError(failure);}
            finally {bitmap.recycle();}
        });
    }
    @Test public void realCloudPushUpdatesBoundWidgetInBackgroundThenDeletionAndLogoutClearIt() throws Exception {
        org.junit.Assume.assumeTrue("Real Firebase requires explicit integration opt-in",
                "true".equals(InstrumentationRegistry.getArguments().getString("taktakRealFcm")));
        var receiver=AuthGraph.repository(context);assertFalse("Không thay tài khoản cá nhân",receiver.hasSession());
        if(android.os.Build.VERSION.SDK_INT>=37) InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),"android.permission.ACCESS_LOCAL_NETWORK");
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),"android.permission.CAMERA");
        var sender=new AuthRepository(new ApiClient(BuildConfig.BASE_URL,new Memory()));
        AppWidgetHost host=new AppWidgetHost(context,65018);int widgetId=0;boolean receiverOwned=false,senderOwned=false;
        var ownedMoments=new ArrayList<String>();
        var created=new CopyOnWriteArrayList<String>();var deleted=new CopyOnWriteArrayList<String>();
        PushEventBus.Listener listener=new PushEventBus.Listener() {
            public void onMomentCreated(String id){created.add(id);}public void onMomentDeleted(String id){deleted.add(id);}
        };
        try {
            phase("create_accounts");
            UserProfile recipient=account(receiver,"Widget Receiver QA");receiverOwned=true;
            UserProfile author=account(sender,"Widget Sender QA");senderOwned=true;
            sender.friends().send(recipient.id);receiver.friends().respond(receiver.friends().incoming(0).items.get(0).id,true);
            phase("register_device");
            String token=Tasks.await(FirebaseMessaging.getInstance().getToken(),60,TimeUnit.SECONDS);
            assertNotNull(token);assertFalse(token.isBlank());receiver.devices(context).register(token);
            widgetId=host.allocateAppWidgetId();
            var component=new ComponentName(context,TaktakAppWidgetProvider.class);var manager=AppWidgetManager.getInstance(context);
            assertTrue("ADB appwidget grantbind required for QA host",manager.bindAppWidgetIdIfAllowed(widgetId,component));
            final int boundId=widgetId;
            PushEventBus.register(listener);
            phase("launch_host");
            if ("true".equals(InstrumentationRegistry.getArguments().getString("taktakRealFcmUsb"))) {
                // Đưa host QA lên foreground như harness USB hiện có, không đổi chính sách MIUI.
                try (var descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation()
                        .executeShellCommand("am start -W -f 0x14000000 -n com.taktak.app/.MainActivity");
                     var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                    String result=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                    assertTrue("USB foreground host started",result.contains("Status: ok") || result.contains("brought to the front"));
                }
                SessionRoutingTest.awaitView(R.id.bottomNavigation);
            }
            try(var scenario=ActivityScenario.launch(MainActivity.class)) {
                SessionRoutingTest.awaitView(R.id.bottomNavigation);
                var reference=new AtomicReference<AppWidgetHostView>();
                scenario.onActivity(activity -> {
                    // Context app dùng inflater framework như launcher, không Factory2 AppCompat của Activity.
                    host.startListening();var widget=host.createView(context,boundId,manager.getAppWidgetInfo(boundId));
                    activity.addContentView(widget,new ViewGroup.LayoutParams(600,600));reference.set(widget);
                });
                var widget=reference.get();
                phase("foreground_upload");
                String first=sender.moments().upload(image(),"Foreground QA",List.of(recipient.id));
                ownedMoments.add(first);
                await(() -> created.contains(first),"Real FCM created event delivered");
                await(() -> visible(widget,R.id.widget_moment_container),"Worker renders real RemoteViews");
                scenario.onActivity(activity -> assertNotNull(((android.widget.ImageView)widget.findViewById(R.id.widget_image)).getDrawable()));
                captureWidget(widget,"foreground");
                phase("background_upload");
                try(var shell=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("input keyevent 3")) { }
                String second=sender.moments().upload(image(),"Background QA",List.of(recipient.id));
                ownedMoments.add(second);
                await(() -> created.contains(second),"Real FCM delivered while launcher is foreground");
                await(() -> {
                    var matches=new AtomicBoolean();InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                        var caption=(android.widget.TextView)widget.findViewById(R.id.widget_caption);
                        matches.set(caption!=null && "Background QA".contentEquals(caption.getText()));
                    });return matches.get();
                },"Background Worker updates caption");
                captureWidget(widget,"background");
                phase("delete_qa_images");
                sender.moments().delete(second);ownedMoments.remove(second);
                sender.moments().delete(first);ownedMoments.remove(first);
                await(() -> deleted.contains(second) && deleted.contains(first),"Real FCM deletion delivered");
                await(() -> {
                    var empty=new AtomicBoolean();InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                        var view=widget.findViewById(R.id.widget_empty_container);empty.set(view!=null && view.getVisibility()==View.VISIBLE);
                    });return empty.get();
                },"Widget removes deleted images");
                captureWidget(widget,"deleted-empty");
                phase("logout_receiver");
                receiver.logout(context);receiverOwned=false;
                await(() -> {
                    var loggedOut=new AtomicBoolean();InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                        var view=widget.findViewById(R.id.widget_logged_out_container);loggedOut.set(view!=null && view.getVisibility()==View.VISIBLE);
                    });return loggedOut.get();
                },"Widget clears on logout");
                captureWidget(widget,"logged-out");
            }
        } finally {
            phase("cleanup");
            // Chỉ xóa đúng ảnh được chính lượt test này tạo, kể cả delivery thất bại.
            for(String id:ownedMoments) {
                try { sender.moments().delete(id); }
                catch(Exception failure) { android.util.Log.w("TaktakOpsQA","QA image cleanup incomplete"); }
            }
            PushEventBus.unregister(listener);host.stopListening();if(widgetId!=0)host.deleteAppWidgetId(widgetId);
            if(receiverOwned)receiver.logout(context);if(senderOwned)sender.logout();
        }
    }
}
