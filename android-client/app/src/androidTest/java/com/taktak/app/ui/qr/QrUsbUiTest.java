package com.taktak.app.ui.qr;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class QrUsbUiTest {
    @Rule public final org.junit.rules.Timeout timeout=org.junit.rules.Timeout.seconds(180);
    private QrFixture fixture;
    private Context context;
    private File sessionFile;
    private byte[] personalSession;
    @Before public void setup()throws Exception {
        assertEquals("true",InstrumentationRegistry.getArguments().getString("taktak.allowQrQa"));
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionFile=new File(context.getApplicationInfo().dataDir,"shared_prefs/auth_session_v1.xml");
        personalSession=sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null;
        fixture=new QrFixture(context);closeScreens();
    }
    @After public void cleanup()throws Exception {
        closeScreens();if(fixture!=null)fixture.close();
        assertArrayEquals("Personal session unchanged",personalSession,sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null);
    }
    @Test public void lookupOfflineRetrySelfMissingMismatchPaginationAndExplicitSend()throws Exception {
        var activity=launch();var model=model(activity);
        await(model::hasCurrentProfile,"QA profile loaded");
        detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_self",QrFixture.SELF));awaitStatus(model,QrScanViewModel.Status.SELF);
        main(()->assertEquals(View.GONE,sheet(activity).findViewById(R.id.btnFoundAddFriend).getVisibility()));closeSheet(activity);
        fixture.offline=true;
        detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER));awaitStatus(model,QrScanViewModel.Status.ERROR);
        assertEquals(0,fixture.sends.get());capture("lookup-offline");fixture.offline=false;
        main(()->sheet(activity).findViewById(R.id.btnFoundAddFriend).performClick());awaitStatus(model,QrScanViewModel.Status.READY);
        assertEquals(0,fixture.sends.get());capture("lookup-ready");closeSheet(activity);
        fixture.missing=true;detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER));awaitStatus(model,QrScanViewModel.Status.NOT_FOUND);
        main(()->assertFalse(sheet(activity).findViewById(R.id.btnFoundAddFriend).isEnabled()));closeSheet(activity);fixture.missing=false;
        detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.SELF));awaitStatus(model,QrScanViewModel.Status.NOT_FOUND);closeSheet(activity);
        fixture.friendOnSecondPage=true;fixture.pages.set(0);
        detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER));awaitStatus(model,QrScanViewModel.Status.FRIEND);assertEquals(2,fixture.pages.get());closeSheet(activity);
        fixture.friendOnSecondPage=false;detect(activity,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER));awaitStatus(model,QrScanViewModel.Status.READY);
        main(()->sheet(activity).findViewById(R.id.btnFoundAddFriend).performClick());await(()->fixture.sends.get()==1,"One explicit request only to RAM");
    }
    @Test public void cameraGalleryMyCodeShareCancelRecreationAndSessionGuard()throws Exception {
        var activity=launch();var model=model(activity);await(model::hasCurrentProfile,"Profile ready");
        await(()->field(activity,"camera")!=null,"Device CameraX bound");capture("camera");
        main(()->activity.findViewById(R.id.btnQrShowMyCode).performClick());
        main(()->{
            assertTrue((Boolean)field(activity,"scanningPaused"));
            var fragment=(MyQrCodeBottomSheet)activity.getSupportFragmentManager().findFragmentByTag("MyQrCode");assertNotNull(fragment);
            var drawable=((android.widget.ImageView)fragment.requireView().findViewById(R.id.qrImageView)).getDrawable();
            assertEquals("qa_qr_self",QrCodeUtils.parsePayload(QrCodeUtils.decodeFromBitmap(((android.graphics.drawable.BitmapDrawable)drawable).getBitmap())).username);
        });capture("my-code");
        var monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
            if(Intent.ACTION_CHOOSER.equals(intent.getAction())){
                Intent send=intent.getParcelableExtra(Intent.EXTRA_INTENT);assertNotNull(send);assertEquals(Intent.ACTION_SEND,send.getAction());assertEquals("text/plain",send.getType());
                assertTrue(send.getStringExtra(Intent.EXTRA_TEXT).contains("taktak://friend?u=qa_qr_self"));return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
            }return null;
        }};
        var instrumentation=InstrumentationRegistry.getInstrumentation();instrumentation.addMonitor(monitor);
        try{main(()->((MyQrCodeBottomSheet)activity.getSupportFragmentManager().findFragmentByTag("MyQrCode")).requireView().findViewById(R.id.btnShareQr).performClick());
            assertEquals("Share chooser intercepted, not sent externally",1,monitor.getHits());}
        finally{instrumentation.removeMonitor(monitor);}
        main(()->((MyQrCodeBottomSheet)activity.getSupportFragmentManager().findFragmentByTag("MyQrCode")).dismiss());
        await(()->!(Boolean)field(activity,"scanningPaused"),"Scan resumes after my code");
        File file=new File(context.getCacheDir(),"qr-ui-gallery.png");
        Bitmap image=QrCodeUtils.generateQrBitmap(QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER),512,Color.BLACK,Color.WHITE);
        try(var out=new FileOutputStream(file)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
        try{main(()->invoke(activity,"onGalleryImagePicked",Uri.class,Uri.fromFile(file)));awaitStatus(model,QrScanViewModel.Status.READY);capture("gallery-result");}
        finally{assertTrue(file.delete());}
        closeSheet(activity);
        // Hủy picker thực: không đọc ảnh cá nhân.
        main(()->activity.findViewById(R.id.btnQrPickGallery).performClick());
        await(()->{
            var root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root==null||root.getPackageName()==null)return false;
            String pkg=root.getPackageName().toString();
            return pkg.contains("photopicker")||pkg.endsWith("documentsui");
        },"Actual image picker visible before Back");capture("gallery-picker");
        assertTrue(instrumentation.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));
        await(()->!(Boolean)field(activity,"pickingImage")&&field(activity,"camera")!=null,"Picker cancellation resumes camera");
        main(activity::recreate);QrScanActivity recreated=awaitActivity(QrScanActivity.class,activity);
        await(()->field(recreated,"camera")!=null,"Camera rebound after recreation");
        var retained=model(recreated);detect(recreated,QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER));awaitStatus(retained,QrScanViewModel.Status.READY);
        fixture.client.sessions.beginLogin();com.taktak.app.data.push.PushEventBus.emitSessionCleared();
        await(()->!retained.hasCurrentProfile(),"Session change clears profile");main(retained::send);assertEquals(0,fixture.sends.get());
    }
    @Test public void cameraPermissionDenialAndGrantThroughSystemDialog()throws Exception {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("taktak.allowQrPermissionQa")));
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,
                androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA));
        var activity=launch();
        clickSystemPermission("permission_deny_button");
        await(()->activity.findViewById(R.id.btnQrRetry).isShown(),"Denial shows permission recovery");
        main(()->{
            assertNull(field(activity,"camera"));
            assertTrue(activity.findViewById(R.id.btnQrPickGallery).isEnabled());
            assertTrue(activity.findViewById(R.id.btnQrShowMyCode).isEnabled());
        });capture("permission-denied");
        var model=model(activity);await(model::hasCurrentProfile,"Profile works without Camera permission");
        main(()->activity.findViewById(R.id.btnQrShowMyCode).performClick());
        main(()->{var sheet=(MyQrCodeBottomSheet)activity.getSupportFragmentManager().findFragmentByTag("MyQrCode");assertNotNull(sheet);sheet.dismiss();});
        main(()->activity.findViewById(R.id.btnQrRetry).performClick());
        clickSystemPermission("permission_allow_foreground_only_button");
        await(()->field(activity,"camera")!=null,"Camera binds after actual system permission grant");
        capture("permission-granted");
    }
    private void clickSystemPermission(String resourceSuffix)throws Exception {
        var automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        var service=automation.getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;automation.setServiceInfo(service);
        var button=new AtomicReference<android.view.accessibility.AccessibilityNodeInfo>();
        await(()->{
            var root=automation.getRootInActiveWindow();
            if(root==null)return false;
            for(String pkg:new String[]{"com.google.android.permissioncontroller","com.android.permissioncontroller"}){
                var nodes=root.findAccessibilityNodeInfosByViewId(pkg+":id/"+resourceSuffix);
                if(!nodes.isEmpty()){button.set(nodes.get(0));return true;}
            }
            return false;
        },"System permission button "+resourceSuffix);
        assertTrue(button.get().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));
    }
    @Test public void usbOpticalTorchAndActualGallerySelection()throws Exception {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("taktak.allowQrOpticalQa")));
        String raw=QrCodeUtils.buildFriendPayload("qa_qr_alice",QrFixture.PEER);
        File folder=new File(context.getExternalFilesDir(null),"artifacts/qr");assertTrue(folder.isDirectory()||folder.mkdirs());
        Bitmap target=QrCodeUtils.generateQrBitmap(raw,768,Color.BLACK,Color.WHITE);
        try(var out=new FileOutputStream(new File(folder,"qr-optical-target.png"))){assertTrue(target.compress(Bitmap.CompressFormat.PNG,100,out));}finally{target.recycle();}
        Bitmap gallery=QrCodeUtils.generateQrBitmap(QrCodeUtils.buildFriendPayload("qa_qr_gallery",QrFixture.GALLERY),768,Color.BLACK,Color.WHITE);
        try(var out=new FileOutputStream(new File(folder,"qr-gallery-target.png"))){assertTrue(gallery.compress(Bitmap.CompressFormat.PNG,100,out));}finally{gallery.recycle();}
        var activity=launch();var model=model(activity);
        await(()->field(activity,"camera")!=null,"USB CameraX bound");
        String phase=InstrumentationRegistry.getArguments().getString("taktak.qrHardwarePhase", "combined");
        if(!"gallery".equals(phase)) {
        var camera=(androidx.camera.core.Camera)field(activity,"camera");assertTrue("Phone has torch",camera.getCameraInfo().hasFlashUnit());
        main(()->activity.findViewById(R.id.btnQrFlash).performClick());
        await(()->Integer.valueOf(androidx.camera.core.TorchState.ON).equals(camera.getCameraInfo().getTorchState().getValue()),"Physical torch on");
        android.os.SystemClock.sleep(600);
        main(()->activity.findViewById(R.id.btnQrFlash).performClick());
        await(()->Integer.valueOf(androidx.camera.core.TorchState.OFF).equals(camera.getCameraInfo().getTorchState().getValue()),"Physical torch off");
        long deadline=android.os.SystemClock.uptimeMillis()+90000;
        while(android.os.SystemClock.uptimeMillis()<deadline){var state=model.state().getValue();if(state!=null&&state.status()==QrScanViewModel.Status.READY)break;android.os.SystemClock.sleep(100);}
        assertNotNull("Actual optical scan returned an identity",model.state().getValue());
        assertEquals(QrScanViewModel.Status.READY,model.state().getValue().status());assertEquals(QrFixture.PEER,model.state().getValue().target().id);
        capture("usb-optical-resolved");assertEquals(0,fixture.sends.get());
        if("optical".equals(phase))return;
        main(()->{sheet(activity).findViewById(R.id.btnFoundCancel).performClick();activity.findViewById(R.id.btnQrPickGallery).performClick();});
        } else {
            main(()->activity.findViewById(R.id.btnQrPickGallery).performClick());
        }
        // Người dùng chọn đúng ảnh QA tạm trong Downloads; không inject callback/URI.
        long deadline=android.os.SystemClock.uptimeMillis()+90000;
        while(android.os.SystemClock.uptimeMillis()<deadline){var state=model.state().getValue();if(state!=null&&state.status()==QrScanViewModel.Status.READY&&"qa_qr_gallery".equals(state.payload().username)&&!(Boolean)field(activity,"pickingImage")&&!(Boolean)field(activity,"readingImage"))break;android.os.SystemClock.sleep(100);}
        assertNotNull("Actual picker delivered a QR image",model.state().getValue());
        assertEquals(QrScanViewModel.Status.READY,model.state().getValue().status());assertEquals(QrFixture.GALLERY,model.state().getValue().target().id);
        assertEquals("qa_qr_gallery",model.state().getValue().payload().username);
        capture("usb-picker-resolved");assertEquals(0,fixture.sends.get());
    }
    private QrScanActivity launch()throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        String result;
        try(var descriptor=instrumentation.getUiAutomation().executeShellCommand("am start -W -f 0x14000000 -n com.taktak.app/.MainActivity");
            var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
            var output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[1024];int count;while((count=stream.read(buffer))!=-1)output.write(buffer,0,count);
            result=output.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
        assertTrue("Main launch must succeed: "+result,result.matches("(?s).*Status:\\s*ok\\b.*"));
        assertFalse("Do not reuse a stale Activity instance",result.contains("Activity not started"));
        var parent=awaitActivity(MainActivity.class,null);
        main(()->parent.startActivity(new Intent(parent,QrScanActivity.class)));return awaitActivity(QrScanActivity.class,null);
    }
    private QrScanViewModel model(QrScanActivity activity){var ref=new AtomicReference<QrScanViewModel>();main(()->ref.set(new ViewModelProvider(activity).get(QrScanViewModel.class)));return ref.get();}
    private void detect(QrScanActivity activity,String value){main(()->invoke(activity,"onQrCodeDetected",String.class,value));}
    private View sheet(QrScanActivity activity){return ((com.google.android.material.bottomsheet.BottomSheetDialog)field(activity,"foundDialog")).getWindow().getDecorView();}
    private void closeSheet(QrScanActivity activity)throws Exception{main(()->sheet(activity).findViewById(R.id.btnFoundCancel).performClick());await(()->field(activity,"foundDialog")==null,"Sheet closed");}
    private void awaitStatus(QrScanViewModel model,QrScanViewModel.Status status)throws Exception{await(()->{var value=model.state().getValue();return value!=null&&value.status()==status;},"QR status "+status);}
    private Object field(Object target,String name){try{var declared=target.getClass().getDeclaredField(name);declared.setAccessible(true);return declared.get(target);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(Object target,String name,Class<?> type,Object value){try{var method=target.getClass().getDeclaredMethod(name,type);method.setAccessible(true);method.invoke(target,value);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void await(BooleanSupplier check,String message)throws Exception{long until=android.os.SystemClock.uptimeMillis()+15000;while(android.os.SystemClock.uptimeMillis()<until){if(check.getAsBoolean())return;android.os.SystemClock.sleep(80);}fail(message);}
    private <T extends Activity>T awaitActivity(Class<T> type,Activity previous)throws Exception{
        var result=new AtomicReference<T>();await(()->{main(()->{for(Activity item:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(type.isInstance(item)&&item!=previous)result.set(type.cast(item));});return result.get()!=null;},"Foreground "+type.getSimpleName());return result.get();
    }
    private void closeScreens(){main(()->{var monitor=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();for(var stage:androidx.test.runner.lifecycle.Stage.values())for(Activity item:new ArrayList<>(monitor.getActivitiesInStage(stage)))if(item.getPackageName().equals(context.getPackageName()))item.finish();});InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void capture(String name)throws Exception{
        android.os.SystemClock.sleep(250);Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        File folder=new File(context.getExternalFilesDir(null),"artifacts/qr");assertTrue(folder.isDirectory()||folder.mkdirs());
        try(var output=new FileOutputStream(new File(folder,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));}finally{image.recycle();}
    }
}
