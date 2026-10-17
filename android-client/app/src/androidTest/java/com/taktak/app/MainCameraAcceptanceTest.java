package com.taktak.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.view.ViewCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.ui.camera.CameraFragment;
import com.taktak.app.ui.camera.PhotoProcessor;
import com.taktak.app.viewmodel.CameraSettingsViewModel;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class MainCameraAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(180);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa5a_fixture", Context.MODE_PRIVATE);
    private boolean ownsSession;
    private static final int[] TABS = {R.id.nav_camera, R.id.nav_history, R.id.nav_friends, R.id.nav_profile};

    @Before public void preservePersonalSession() throws Exception {
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            String owner = fixture.getString("owner", "");
            assertFalse("Refuse personal session", owner.isEmpty());
            assertEquals(owner, AuthGraph.repository(context).profile().id);
            AuthGraph.repository(context).logout();
        }
        assertTrue(fixture.edit().clear().commit());
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }
    @After public void cleanupOwnSession() throws Exception {
        try {
            if (ownsSession) { AuthGraph.repository(context).logout(); assertTrue(fixture.edit().clear().commit()); }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : new ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))) { activity.finish(); }
            });
        }
    }

    @Test public void fourTabShellFitsLargeTextAndBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                var config = new Configuration(context.getResources().getConfiguration()); config.fontScale = 1.5f;
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_TakTak_M3);
                float density = themed.getResources().getDisplayMetrics().density;
                for (int[] size : new int[][]{{280, 560}, {360, 800}, {840, 420}}) {
                    View root = LayoutInflater.from(themed).inflate(R.layout.activity_main, null, false);
                    BottomNavigationView nav = root.findViewById(R.id.bottomNavigation); nav.setVisibility(View.VISIBLE);
                    assertEquals(4, nav.getMenu().size());
                    for (int i = 0; i < 4; i++) { assertEquals(TABS[i], nav.getMenu().getItem(i).getItemId()); }
                    root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(size[0] * density), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(Math.round(size[1] * density), View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                    bounds(root, root.getWidth());
                    for (int id : TABS) {
                        View item = nav.findViewById(id);
                        assertTrue(item.getHeight() >= Math.round(48 * density)); assertTrue(item.getWidth() >= Math.round(48 * density));
                    }
                    assertEquals(root.getHeight(), nav.getBottom());
                }
            }
        });
    }

    @Test public void tabsRecreationBackAndLogoutNeverKeepCameraBoundOffscreen() throws Exception {
        login(); launch(); readyCamera(); capture("camera-tab.png");
        AtomicReference<CameraFragment> old = new AtomicReference<>();
        AtomicReference<Preview> preview = new AtomicReference<>();
        onMain(activity -> {
            old.set(MainNavigationTestSupport.camera(activity)); preview.set(field(old.get(), "preview", Preview.class));
        });
        select(R.id.nav_history); SessionRoutingTest.awaitView(R.id.historyPage);
        onMain(activity -> {
            assertNull(field(old.get(), "capture", ImageCapture.class));
            assertFalse(field(old.get(), "provider", ProcessCameraProvider.class).isBound(preview.get()));
            assertEquals(context.getString(R.string.history_title),
                    ((TextView) activity.findViewById(R.id.historyPage).findViewById(R.id.historyTitle)).getText().toString());
        });
        capture("history-tab.png");
        select(R.id.nav_friends); SessionRoutingTest.awaitView(R.id.friendsQuery);
        capture("friends-tab.png");
        select(R.id.nav_profile); SessionRoutingTest.awaitView(R.id.profilePage); capture("profile-tab.png");
        onMain(MainActivity::recreate);
        SessionRoutingTest.awaitView(R.id.bottomNavigation); awaitTab(R.id.nav_profile);
        onMain(activity -> {
            assertNull(MainNavigationTestSupport.camera(activity));
            activity.getOnBackPressedDispatcher().onBackPressed();
        });
        readyCamera();
        onView(withId(R.id.mainPager)).perform(androidx.test.espresso.action.ViewActions.swipeLeft());
        awaitTab(R.id.nav_history);
        onMain(activity -> activity.findViewById(R.id.logoutButton).performClick());
        SessionRoutingTest.awaitView(R.id.emailInput);
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        SessionRoutingTest.onResumed(activity -> {
            assertTrue(activity.isTaskRoot()); assertNull(activity.findViewById(R.id.bottomNavigation));
        });
        capture("logout.png");
    }

    @Test public void frontRearFlashAndRealPrivatePhotosSurviveRecreation() throws Exception {
        login(); launch(); readyCamera();
        AtomicBoolean flash = new AtomicBoolean(), switchable = new AtomicBoolean();
        onMain(activity -> {
            flash.set(activity.findViewById(R.id.flashButton).isEnabled());
            switchable.set(activity.findViewById(R.id.switchCameraButton).isEnabled());
            assertEquals(CameraSelector.LENS_FACING_BACK, settings(activity).lensFacing());
            assertEquals(ImageCapture.FLASH_MODE_OFF, settings(activity).flashMode());
        });
        android.util.Log.i("TaktakQa", "5A_CAPABILITIES flash=" + flash.get() + " switch=" + switchable.get());
        if (flash.get()) {
            for (int mode : new int[]{ImageCapture.FLASH_MODE_AUTO, ImageCapture.FLASH_MODE_ON, ImageCapture.FLASH_MODE_OFF}) {
                cameraButton(R.id.flashButton);
                onMain(activity -> {
                    assertEquals(mode, settings(activity).flashMode());
                    assertEquals(mode, field(MainNavigationTestSupport.camera(activity), "capture", ImageCapture.class).getFlashMode());
                });
            }
            cameraButton(R.id.flashButton); cameraButton(R.id.flashButton);
        }
        captureAndVerify("rear-photo.png");
        if (switchable.get()) {
            cameraButton(R.id.switchCameraButton); readyCamera();
            onMain(activity -> {
                assertEquals(CameraSelector.LENS_FACING_FRONT, settings(activity).lensFacing());
                var camera = MainNavigationTestSupport.camera(activity);
                assertEquals(field(camera, "hasFlash", Boolean.class).booleanValue(), activity.findViewById(R.id.flashButton).isEnabled());
            });
            capture("front-preview.png"); captureAndVerify("front-photo.png");
            onMain(MainActivity::recreate); readyCamera();
            onMain(activity -> assertEquals(CameraSelector.LENS_FACING_FRONT, settings(activity).lensFacing()));
            cameraButton(R.id.switchCameraButton); readyCamera();
            onMain(activity -> assertEquals(CameraSelector.LENS_FACING_BACK, settings(activity).lensFacing()));
        } else {
            onMain(activity -> assertFalse(activity.findViewById(R.id.switchCameraButton).isEnabled()));
        }
    }

    @Test public void backgroundResumeRechecksSessionAndReleasesUseCases() throws Exception {
        login(); launch(); readyCamera();
        AtomicReference<CameraFragment> old = new AtomicReference<>();
        onMain(activity -> old.set(MainNavigationTestSupport.camera(activity)));
        shell("input keyevent KEYCODE_HOME");
        await(() -> {
            AtomicBoolean stopped = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> stopped.set(field(old.get(), "capture", ImageCapture.class) == null));
            return stopped.get();
        }, "Camera unbound in background");
        launch(); readyCamera(); capture("foreground-restored.png");
    }

    @Test public void activityOrientationChangesKeepSquarePreviewAndSelectedLens() throws Exception {
        login(); launch(); readyCamera();
        AtomicInteger previous = new AtomicInteger(), lens = new AtomicInteger();
        onMain(activity -> { previous.set(activity.getRequestedOrientation()); lens.set(settings(activity).lensFacing()); });
        try {
            for (int requested : new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}) {
                int expected = requested == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
                onMain(activity -> activity.setRequestedOrientation(requested));
                await(() -> {
                    AtomicBoolean matches = new AtomicBoolean();
                    onMain(activity -> matches.set(activity.getResources().getConfiguration().orientation == expected));
                    return matches.get();
                }, "Activity configuration changes orientation");
                readyCamera();
                onMain(activity -> {
                    View preview = activity.findViewById(R.id.cameraPreview);
                    assertTrue(Math.abs(preview.getWidth() - preview.getHeight()) <= 1);
                    assertEquals(lens.get(), settings(activity).lensFacing());
                    assertTrue(activity.findViewById(R.id.bottomNavigation).isShown());
                });
                capture(expected == Configuration.ORIENTATION_LANDSCAPE ? "landscape.png" : "portrait.png");
            }
        } finally { onMain(activity -> activity.setRequestedOrientation(previous.get())); }
    }

    @Test public void jpegRotationKeepsAllFourCornerOrientations() throws Exception {
        int[][] expected = {{Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW}, {Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN},
                {Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED}, {Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE}};
        Bitmap source = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
        var canvas = new Canvas(source); var paint = new Paint();
        paint.setColor(Color.RED); canvas.drawRect(0, 0, 200, 150, paint);
        paint.setColor(Color.GREEN); canvas.drawRect(200, 0, 400, 150, paint);
        paint.setColor(Color.BLUE); canvas.drawRect(0, 150, 200, 300, paint);
        paint.setColor(Color.YELLOW); canvas.drawRect(200, 150, 400, 300, paint);
        try {
            for (int i = 0; i < 4; i++) {
                byte[] jpeg = PhotoProcessor.squareJpeg(source, i * 90);
                assertTrue(jpeg.length <= 800 * 1024); assertFalse(source.isRecycled());
                Bitmap image = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                assertEquals(image.getWidth(), image.getHeight());
                int a = image.getWidth() / 4, b = image.getWidth() * 3 / 4;
                int[] pixels = {image.getPixel(a, a), image.getPixel(b, a), image.getPixel(a, b), image.getPixel(b, b)};
                for (int corner = 0; corner < 4; corner++) {
                    assertTrue(Math.abs(Color.red(expected[i][corner]) - Color.red(pixels[corner])) < 20);
                    assertTrue(Math.abs(Color.green(expected[i][corner]) - Color.green(pixels[corner])) < 20);
                    assertTrue(Math.abs(Color.blue(expected[i][corner]) - Color.blue(pixels[corner])) < 20);
                }
                image.recycle();
            }
        } finally { source.recycle(); }
    }

    private void login() throws Exception {
        var repository = AuthGraph.repository(context);
        String username = "qa5a_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
        var user = repository.register(username + "@example.test", username, "5A QA", password);
        assertTrue(fixture.edit().putString("owner", user.id).commit()); ownsSession = true;
        repository.login(username + "@example.test", password);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), Manifest.permission.CAMERA);
    }
    private void launch() throws Exception { shell("am start -W -n com.taktak.app/.MainActivity"); }
    private void select(int id) { onView(withId(id)).perform(click()); awaitTab(id); }
    private void awaitTab(int id) {
        await(() -> {
            AtomicBoolean selected = new AtomicBoolean();
            onMain(activity -> {
                int index = 0; while (TABS[index] != id) { index++; }
                selected.set(((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).getSelectedItemId() == id
                        && ((ViewPager2) activity.findViewById(R.id.mainPager)).getCurrentItem() == index);
            });
            return selected.get();
        }, "Expected selected tab");
    }
    private void readyCamera() {
        await(() -> {
            AtomicBoolean ready = new AtomicBoolean();
            onMain(activity -> {
                var camera = MainNavigationTestSupport.camera(activity);
                PreviewView view = activity.findViewById(R.id.cameraPreview);
                ready.set(activity.isCameraPageActive() && camera != null && camera.isResumed()
                        && field(camera, "capture", ImageCapture.class) != null && view != null
                        && view.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING
                        && activity.findViewById(R.id.captureButton).isEnabled());
            }); return ready.get();
        }, "Selected camera preview streaming");
    }
    private void cameraButton(int id) {
        onView(withId(id)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click());
    }
    private void captureAndVerify(String name) throws Exception {
        onMain(activity -> {
            activity.findViewById(R.id.captureButton).performClick();
            assertFalse(activity.findViewById(R.id.captureButton).isEnabled());
            assertFalse(activity.findViewById(R.id.switchCameraButton).isEnabled());
            assertFalse(activity.findViewById(R.id.flashButton).isEnabled());
        });
        PreviewTestSupport.ready();
        PreviewTestSupport.onPreview(activity -> PreviewTestSupport.model(activity).savePrivate());
        AtomicReference<byte[]> bytes = new AtomicReference<>(); AtomicReference<String> id = new AtomicReference<>();
        await(() -> {
            AtomicBoolean done = new AtomicBoolean();
            PreviewTestSupport.onPreview(activity -> {
                var model = PreviewTestSupport.model(activity);
                var state = model.state().getValue();
                if (!state.sending() && state.downloaded() != null) {
                    bytes.set(state.downloaded()); id.set(field(model, "momentId", String.class));
                    assertTrue(activity.findViewById(R.id.previewImage).isShown()); done.set(true);
                }
            }); return done.get();
        }, "Private JPEG roundtrip");
        assertArrayEquals(AuthGraph.repository(context).moments().download(id.get()), bytes.get());
        Bitmap image = BitmapFactory.decodeByteArray(bytes.get(), 0, bytes.get().length);
        assertNotNull(image); assertEquals(image.getWidth(), image.getHeight()); image.recycle(); capture(name);
        PreviewTestSupport.onPreview(android.app.Activity::finish); readyCamera();
    }
    private CameraSettingsViewModel settings(MainActivity activity) {
        return new androidx.lifecycle.ViewModelProvider(activity).get(CameraSettingsViewModel.class);
    }
    private <T> T field(Object target, String name, Class<T> type) {
        try { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(target)); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private void onMain(java.util.function.Consumer<MainActivity> action) {
        long end = SystemClock.elapsedRealtime() + 10000;
        while (SystemClock.elapsedRealtime() < end) {
            AtomicBoolean done = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                var activities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
                assertTrue(activities.size() <= 1);
                if (activities.size() == 1 && activities.iterator().next() instanceof MainActivity activity) { action.accept(activity); done.set(true); }
            });
            if (done.get()) { return; } SystemClock.sleep(50);
        }
        fail("MainActivity resumes");
    }
    private void await(java.util.function.BooleanSupplier condition, String message) {
        long end = SystemClock.elapsedRealtime() + 40000;
        while (SystemClock.elapsedRealtime() < end) { if (condition.getAsBoolean()) { return; } SystemClock.sleep(100); }
        fail(message);
    }
    private void shell(String command) throws Exception {
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
    }
    private void bounds(View view, int width) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof TextView text && text.getLayout() != null) {
            assertTrue("Text fits", text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) { for (int i = 0; i < group.getChildCount(); i++) { bounds(group.getChildAt(i), group.getWidth()); } }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(1000); var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/5a"); assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); }
        finally { bitmap.recycle(); }
    }
}
