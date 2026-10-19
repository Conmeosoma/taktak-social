package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.taktak.app.*;
import com.taktak.app.data.repository.CaptureDraftRepository;
import com.taktak.app.ui.camera.*;
import com.taktak.app.ui.history.MomentViewerActivity;
import com.taktak.app.viewmodel.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chỉ fixture UI/Retrofit trong RAM, không đổi auth store/backend/widget của người dùng. */
@RunWith(AndroidJUnit4.class)
public class MediaThemeUiTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(360);
    private Context context;
    private MediaThemeFixture fixture;
    private Map<String,?> original;
    private File sessionFile;
    private byte[] sessionBytes;
    private boolean foreground;
    private final List<String> ownedDrafts = new ArrayList<>();
    @Before public void setup() throws Exception {
        assertEquals("Explicit USB QA opt-in","true",InstrumentationRegistry.getArguments().getString("taktak.allowMediaQa"));
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(context.getPackageName(), android.Manifest.permission.CAMERA);
        original=new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME,Context.MODE_PRIVATE).getAll());
        sessionFile=new File(context.getApplicationInfo().dataDir,"shared_prefs/auth_session_v1.xml");
        sessionBytes=sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null;
        fixture=new MediaThemeFixture(context);
    }
    @After public void cleanup() throws Exception {
        closeScreens();
        if (fixture!=null) {
            var drafts=new CaptureDraftRepository(context,fixture.auth.sessionGeneration());
            for(String id:ownedDrafts) drafts.discard(id);
            fixture.close();
        }
        if(original!=null) {
            var editor=context.getSharedPreferences(ThemePreferences.FILE_NAME,Context.MODE_PRIVATE).edit();
            for(String key:new String[]{ThemePreferences.STYLE,ThemePreferences.MODE,ThemePreferences.SCHEMA,ThemePreferences.REVISION}) {
                Object value=original.get(key);
                if(!original.containsKey(key)) editor.remove(key);
                else if(value instanceof String s) editor.putString(key,s);
                else if(value instanceof Integer i) editor.putInt(key,i);
                else if(value instanceof Long l) editor.putLong(key,l);
                else throw new AssertionError("Unsupported existing appearance type");
            }
            assertTrue(editor.commit());
            assertEquals(original,context.getSharedPreferences(ThemePreferences.FILE_NAME,Context.MODE_PRIVATE).getAll());
            assertArrayEquals("Personal session bytes unchanged",sessionBytes,sessionFile.exists()
                    ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null);
        }
    }
    @Test public void allStylesModesLayoutsAndOriginalTabContexts() {
        main(() -> {
            for(AppTheme style:AppTheme.values()) for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for(float font:new float[]{1f,1.5f,2f}) for(int[] size:new int[][]{{280,560},{360,720},{840,420}}) {
                    var config=new Configuration(context.getResources().getConfiguration()); config.fontScale=font;
                    config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|night;
                    for(ThemeManager.ScreenRole role:new ThemeManager.ScreenRole[]{ThemeManager.ScreenRole.MAIN,ThemeManager.ScreenRole.PREVIEW,ThemeManager.ScreenRole.VIEWER}) {
                        var themed=new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                                ThemeManager.resolve(new ThemeSelection(style,ColorMode.SYSTEM,0),role).themeResource());
                        int layout=role==ThemeManager.ScreenRole.MAIN ? R.layout.activity_main : role==ThemeManager.ScreenRole.PREVIEW ? R.layout.activity_preview : R.layout.activity_moment_viewer;
                        View root=LayoutInflater.from(themed).inflate(layout,null,false);
                        if(role==ThemeManager.ScreenRole.MAIN) {
                            root.findViewById(R.id.sessionScroll).setVisibility(View.GONE);
                            root.findViewById(R.id.cameraPanel).setVisibility(View.VISIBLE);
                            root.findViewById(R.id.bottomNavigation).setVisibility(View.VISIBLE);
                            View page=LayoutInflater.from(themed).inflate(R.layout.fragment_camera_page,null,false);
                            View slot=page.findViewById(R.id.cameraContent);
                            var parent=(android.view.ViewGroup)slot.getParent(); int index=parent.indexOfChild(slot);
                            var holder=new FrameLayout(slot.getContext()); holder.setLayoutParams(slot.getLayoutParams());
                            parent.removeView(slot); parent.addView(holder,index);
                            holder.addView(LayoutInflater.from(themed).inflate(R.layout.fragment_camera,holder,false));
                            ((FrameLayout)root.findViewById(R.id.cameraPanel)).addView(page);
                            for(int tab:new int[]{R.layout.fragment_history,R.layout.fragment_friends,R.layout.fragment_profile}) {
                                View ported=LayoutInflater.from(themed).inflate(tab,null,false);
                                assertEquals("Ported tab adopts themed context",color(themed,androidx.appcompat.R.attr.colorPrimary),
                                        MaterialColors.getColor(ported,androidx.appcompat.R.attr.colorPrimary));
                            }
                        } else if(role==ThemeManager.ScreenRole.PREVIEW) {
                            root.findViewById(R.id.previewScroll).setVisibility(View.VISIBLE);
                            ((TextView)root.findViewById(R.id.previewNotice)).setText(R.string.moment_transfer_error);
                            ((TextView)root.findViewById(R.id.friendsNotice)).setText(R.string.preview_friends_error);
                            var list=(android.view.ViewGroup)root.findViewById(R.id.previewContent);
                            var item=LayoutInflater.from(themed).inflate(R.layout.item_recipient,list,false);
                            ((TextView)item).setText("QA recipient — username_example"); list.addView(item);
                        } else {
                            ((TextView)root.findViewById(R.id.viewerSenderName)).setText("QA Sender with a longer display name");
                            ((TextView)root.findViewById(R.id.viewerSenderUsername)).setText("@username_example");
                            ((TextView)root.findViewById(R.id.viewerTimestamp)).setText("19/10/2026");
                            root.findViewById(R.id.viewerDeleteButton).setVisibility(View.VISIBLE);
                            ((TextView)root.findViewById(R.id.viewerCaption)).setText("QA caption ".repeat(4));
                            root.findViewById(R.id.viewerProgress).setVisibility(View.GONE);
                            root.findViewById(R.id.viewerErrorView).setVisibility(View.VISIBLE);
                        }
                        float density=themed.getResources().getDisplayMetrics().density;
                        int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                        root.layout(0,0,w,h); bounds(root,w);
                        int bg=role==ThemeManager.ScreenRole.VIEWER ? color(themed,R.attr.ttMediaBackground)
                                : color(themed,com.google.android.material.R.attr.colorSurface);
                        int ink=role==ThemeManager.ScreenRole.VIEWER ? color(themed,R.attr.ttViewerInk)
                                : color(themed,com.google.android.material.R.attr.colorOnSurface);
                        assertTrue("Media body contrast",androidx.core.graphics.ColorUtils.calculateContrast(ink,bg)>=4.5);
                    }
                }
        });
    }
    @Test public void cameraNavigationAndPendingThemeKeepActivityTabAndSettings() throws Exception {
        for(AppTheme style:AppTheme.values()) for(ColorMode mode:new ColorMode[]{ColorMode.LIGHT,ColorMode.DARK}) {
            select(style,mode); MainActivity activity=launch(MainActivity.class); authorize(activity);
            capture("camera-"+style.id()+"-"+mode.id());
            main(() -> {
                var nav=(BottomNavigationView)activity.findViewById(R.id.bottomNavigation);
                assertEquals(5,nav.getMenu().size());
                for(int tab:new int[]{R.id.nav_history,R.id.nav_messages,R.id.nav_friends,R.id.nav_profile,R.id.nav_camera}) {
                    nav.setSelectedItemId(tab); assertEquals(tab,nav.getSelectedItemId());
                }
            });
            closeScreens();
        }
        select(AppTheme.GRADIENT,ColorMode.LIGHT);
        MainActivity activity=launch(MainActivity.class); authorize(activity);
        main(() -> ((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_friends));
        var settings=new AtomicReference<CameraSettingsViewModel>();
        main(() -> {
            settings.set(new ViewModelProvider(activity).get(CameraSettingsViewModel.class));
            settings.get().selectLens(androidx.camera.core.CameraSelector.LENS_FACING_FRONT); settings.get().cycleFlash();
            activity.startActivity(new Intent(activity,ThemeSettingsActivity.class));
        });
        ThemeSettingsActivity chooser=awaitActivity(ThemeSettingsActivity.class,null);
        select(AppTheme.SOFT,ColorMode.LIGHT); main(chooser::finish);
        await(() -> foreground(activity),"Return to original Main");
        authorize(activity);
        main(() -> {
            assertEquals(View.VISIBLE,activity.findViewById(R.id.themeMediaNotice).getVisibility());
            assertEquals(R.id.nav_friends,((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).getSelectedItemId());
            assertSame(settings.get(),new ViewModelProvider(activity).get(CameraSettingsViewModel.class));
        });
        main(activity::recreate);
        MainActivity recreated=awaitActivity(MainActivity.class,activity); authorize(recreated);
        main(() -> {
            assertEquals(R.id.nav_friends,((BottomNavigationView)recreated.findViewById(R.id.bottomNavigation)).getSelectedItemId());
            assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_FRONT,new ViewModelProvider(recreated).get(CameraSettingsViewModel.class).lensFacing());
            assertEquals(androidx.camera.core.ImageCapture.FLASH_MODE_AUTO,new ViewModelProvider(recreated).get(CameraSettingsViewModel.class).flashMode());
        });
        assertEquals(0,fixture.posts.get()); assertEquals(0,fixture.deletes.get());
    }
    @Test public void previewDraftCaptionSelectionFrozenRetryKeyAndImageSurviveRecreation() throws Exception {
        for(AppTheme style:AppTheme.values()) {
            select(style,ColorMode.LIGHT); fixture.posts.set(0); fixture.keys.clear(); fixture.captions.clear(); fixture.recipients.clear(); fixture.failFirstPost=true;
            var drafts=new CaptureDraftRepository(context,fixture.auth.sessionGeneration());
            String id=drafts.write(fixture.jpeg); ownedDrafts.add(id); File file=drafts.file(id);
            Intent intent=new Intent(context,PreviewActivity.class).putExtra(PreviewActivity.DRAFT_ID,id)
                    .putExtra(PreviewActivity.GENERATION,fixture.auth.sessionGeneration());
            PreviewActivity activity=launch(PreviewActivity.class,intent); authorize(activity); readyPreview(activity);
            String caption=new String(Character.toChars(0x1F642)).repeat(50);
            main(() -> {
                ((EditText)activity.findViewById(R.id.captionInput)).setText(caption+caption);
                assertEquals(caption,preview(activity).caption());
                preview(activity).selectAll(false); preview(activity).toggle(MediaThemeFixture.FIRST,true);
            });
            capture("preview-"+style.id());
            main(() -> activity.findViewById(R.id.sendFriendsButton).performClick());
            await(() -> previewState(activity).sending(),"Upload starts and locks UI");
            main(() -> assertFalse(activity.findViewById(R.id.retakeButton).isEnabled()));
            await(() -> previewState(activity).retry(),"Retry retains payload");
            String key=fixture.keys.get(0);
            main(() -> {
                preview(activity).caption("must not replace frozen caption"); preview(activity).selectAll(true);
                assertEquals(caption,preview(activity).caption()); assertFalse(previewState(activity).all());
                assertEquals(Set.of(MediaThemeFixture.FIRST),previewState(activity).selected());
                assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled());
            });
            capture("retry-"+style.id());
            assertTrue(file.isFile()); main(activity::recreate);
            PreviewActivity recreated=awaitActivity(PreviewActivity.class,activity); authorize(recreated); readyPreview(recreated);
            main(() -> {
                assertEquals(caption,((EditText)recreated.findViewById(R.id.captionInput)).getText().toString());
                assertEquals(Set.of(MediaThemeFixture.FIRST),previewState(recreated).selected());
                assertTrue(preview(recreated).selectionLocked()); assertTrue(previewState(recreated).retry());
                recreated.findViewById(R.id.sendFriendsButton).performClick();
            });
            await(() -> previewState(recreated).downloaded()!=null,"Retry succeeds with fixture bytes");
            assertEquals(2,fixture.posts.get()); assertEquals(key,fixture.keys.get(1));
            assertEquals(List.of(caption,caption),fixture.captions);
            assertEquals(List.of(MediaThemeFixture.FIRST,MediaThemeFixture.FIRST),fixture.recipients);
            assertArrayEquals(fixture.jpeg,previewState(recreated).downloaded()); assertFalse(file.exists());
            capture("sent-fixture-"+style.id()); closeScreens();
        }
    }
    @Test public void viewerAllStylesModesDialogAndRevocationKeepPrivatePhotoHidden() throws Exception {
        for(AppTheme style:AppTheme.values()) for(ColorMode mode:new ColorMode[]{ColorMode.LIGHT,ColorMode.DARK}) {
            select(style,mode); fixture.denyImage=false;
            Intent intent=new Intent(context,MomentViewerActivity.class).putExtra(MomentViewerActivity.EXTRA_MOMENT_ID,MediaThemeFixture.MOMENT)
                    .putExtra(MomentViewerActivity.EXTRA_SENDER_NAME,"QA Sender").putExtra(MomentViewerActivity.EXTRA_SENDER_USERNAME,"qa_sender")
                    .putExtra(MomentViewerActivity.EXTRA_CAPTION,"QA fixture photo").putExtra(MomentViewerActivity.EXTRA_IS_SENDER,true);
            MomentViewerActivity activity=launch(MomentViewerActivity.class,intent);
            await(() -> enabled(activity,R.id.viewerSaveButton),"Fixture bitmap visible");
            capture("viewer-"+style.id()+"-"+mode.id());
            main(() -> activity.findViewById(R.id.viewerDeleteButton).performClick());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.moment_delete_cancel))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            assertEquals(0,fixture.deletes.get());
            fixture.denyImage=true;
            main(() -> activity.startActivity(new Intent(activity,ThemeSettingsActivity.class)));
            ThemeSettingsActivity chooser=awaitActivity(ThemeSettingsActivity.class,null);
            await(() -> {
                var stopped=new AtomicBoolean();
                main(() -> {
                    var stage=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity);
                    stopped.set(stage==androidx.test.runner.lifecycle.Stage.STOPPED || stage==androidx.test.runner.lifecycle.Stage.DESTROYED);
                });
                return stopped.get();
            },"Viewer leaves foreground before revocation revalidation");
            main(chooser::finish);
            MomentViewerActivity resumed=awaitActivity(MomentViewerActivity.class,null);
            await(() -> visible(resumed,R.id.viewerErrorView),"403 hides photo");
            main(() -> {
                assertNull(((ImageView)resumed.findViewById(R.id.viewerImage)).getDrawable());
                assertFalse(resumed.findViewById(R.id.viewerSaveButton).isEnabled());
                assertFalse(resumed.findViewById(R.id.viewerDeleteButton).isEnabled());
                assertEquals(View.INVISIBLE,resumed.findViewById(R.id.viewerSenderName).getVisibility());
            });
            capture("viewer-denied-"+style.id()+"-"+mode.id()); closeScreens();
        }
        assertEquals(0,fixture.posts.get()); assertEquals(0,fixture.deletes.get());
    }
    @Test public void oneApprovedRealCaptureToCacheAndRetakeWithoutUpload() throws Exception {
        assertEquals("User approved one QA capture","true",InstrumentationRegistry.getArguments().getString("taktak.captureQa"));
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(android.Manifest.permission.CAMERA));
        select(AppTheme.GLASS,ColorMode.SYSTEM); MainActivity activity=launch(MainActivity.class); authorize(activity);
        await(() -> enabled(activity,R.id.captureButton),"CameraX ready");
        main(() -> activity.findViewById(R.id.captureButton).performClick());
        PreviewActivity captured=awaitActivity(PreviewActivity.class,null); authorize(captured); readyPreview(captured);
        File file=previewState(captured).file(); assertNotNull(file);
        var options=new BitmapFactory.Options(); options.inJustDecodeBounds=true; BitmapFactory.decodeFile(file.getPath(),options);
        assertTrue(options.outWidth>0); assertEquals(options.outWidth,options.outHeight);
        capture("approved-camera-cache");
        main(() -> captured.findViewById(R.id.retakeButton).performClick());
        await(() -> !file.exists(),"Retake discards only owned draft");
        assertEquals(0,fixture.posts.get()); assertEquals(0,fixture.deletes.get());
    }
    private int color(Context themed,int attr) { return MaterialColors.getColor(themed,attr,"QA"); }
    private void bounds(View view,int width) {
        if(view.getVisibility()==View.GONE) return;
        String idName;
        try { idName = view.getId() != View.NO_ID ? view.getResources().getResourceEntryName(view.getId()) : "none"; }
        catch (Exception e) { idName = "none"; }
        assertTrue("Horizontal bounds: "+view.getClass().getSimpleName()+" ["+idName+"] left="+view.getLeft()+" right="+view.getRight()+" max="+width,
                view.getLeft()>=0 && view.getRight()<=width);
        if(view instanceof TextView text && !(text instanceof EditText) && text.getText() != null && text.getText().length() > 0 && text.getLayout()!=null)
            assertTrue("Text fits height: "+view.getClass().getSimpleName()+" ["+idName+"]",
                    text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        if(view instanceof android.view.ViewGroup group) for(int i=0;i<group.getChildCount();i++) bounds(group.getChildAt(i),group.getWidth());
    }
    private void select(AppTheme style,ColorMode mode) throws Exception { new ThemePreferences(context).save(style,mode); }
    private void warm() throws Exception {
        if(foreground) return;
        try(var descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
            var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) { byte[] buffer=new byte[1024]; while(input.read(buffer)!=-1) { } }
        foreground=true;
    }
    private <T extends Activity> T launch(Class<T> type) throws Exception { return launch(type,new Intent(context,type)); }
    private <T extends Activity> T launch(Class<T> type,Intent intent) throws Exception {
        warm();
        if(type==MainActivity.class) {
            T result=awaitActivity(type,null);
            await(() -> foreground(result),"Main shell has focus after USB shell launch");
            return result;
        }
        var result=type.cast(InstrumentationRegistry.getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        await(() -> foreground(result),"Screen has focus"); return result;
    }
    private <T extends Activity> T awaitActivity(Class<T> type,Activity previous) throws Exception {
        var result=new AtomicReference<T>();
        await(() -> {
            main(() -> {
                for(var activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
                    if(type.isInstance(activity) && activity!=previous) result.set(type.cast(activity));
            }); return result.get()!=null;
        },"Activity resumes: "+type.getSimpleName());
        return result.get();
    }
    @SuppressWarnings("unchecked") private void authorize(Activity activity) throws Exception {
        await(() -> {
            var state=new AtomicReference<SessionViewModel.State>();
            main(() -> state.set(new ViewModelProvider((ViewModelStoreOwner)activity).get(SessionViewModel.class).state().getValue()));
            return state.get() != null && (state.get().destination == SessionViewModel.Destination.ERROR || state.get().destination == SessionViewModel.Destination.CAMERA);
        },"Mock profile blocked before FCM/widget");
        main(() -> ((MutableLiveData<SessionViewModel.State>)new ViewModelProvider((ViewModelStoreOwner)activity)
                .get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
    }
    private PreviewViewModel preview(PreviewActivity activity) { return new ViewModelProvider(activity).get(PreviewViewModel.class); }
    private PreviewViewModel.State previewState(PreviewActivity activity) {
        var result=new AtomicReference<PreviewViewModel.State>(); main(() -> result.set(preview(activity).state().getValue())); return result.get();
    }
    private void readyPreview(PreviewActivity activity) throws Exception {
        await(() -> { var state=previewState(activity); return state.file()!=null && !state.loading() && !state.loadingFriends() && state.friends().size()==2; },"Preview draft and fixture recipients ready");
    }
    private boolean enabled(Activity activity,int id) { var result=new AtomicBoolean(); main(() -> { View view=activity.findViewById(id); result.set(view!=null && view.isEnabled()); }); return result.get(); }
    private boolean visible(Activity activity,int id) { var result=new AtomicBoolean(); main(() -> result.set(activity.findViewById(id).isShown())); return result.get(); }
    private boolean foreground(Activity activity) { var result=new AtomicBoolean(); main(() -> result.set(!activity.isFinishing() && activity.getWindow().getDecorView().hasWindowFocus())); return result.get(); }
    private void await(BooleanSupplier condition,String message) throws Exception {
        long until=android.os.SystemClock.uptimeMillis()+18000;
        while(android.os.SystemClock.uptimeMillis()<until) { if(condition.getAsBoolean()) return; android.os.SystemClock.sleep(80); }
        fail(message);
    }
    private void main(Runnable action) {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()) action.run();
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
    private void closeScreens() {
        main(() -> {
            var monitor=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();
            for(var stage:androidx.test.runner.lifecycle.Stage.values())
                for(Activity activity:new ArrayList<>(monitor.getActivitiesInStage(stage)))
                    if(activity instanceof MainActivity || activity instanceof PreviewActivity || activity instanceof MomentViewerActivity || activity instanceof ThemeSettingsActivity) activity.finish();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); foreground=false;
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(500);
        var image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(image);
        var folder=new File(context.getExternalFilesDir(null),"artifacts/theme06"); assertTrue(folder.isDirectory() || folder.mkdirs());
        try(var out=new FileOutputStream(new File(folder,name+".png"))) { assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out)); }
        finally { image.recycle(); }
    }
}
