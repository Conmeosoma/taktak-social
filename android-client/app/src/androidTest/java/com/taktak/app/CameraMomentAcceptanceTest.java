package com.taktak.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.*;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.ui.camera.CameraFragment;
import com.taktak.app.ui.camera.PhotoProcessor;
import com.taktak.app.viewmodel.CaptureViewModel;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CameraMomentAcceptanceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private boolean ownsSession;
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa3d_fixture", Context.MODE_PRIVATE);

    @Before public void localNetworkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }

    @After public void cleanup() throws Exception {
        if (ownsSession) {
            try { AuthGraph.repository(context).logout(); }
            finally { assertTrue(fixture.edit().remove("userId").commit()); }
        }
    }

    private AuthRepository loginAndLaunch(boolean grantCamera) throws Exception {
        var repository = AuthGraph.repository(context);
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            assertEquals("Preserve a session not owned by this fixture", fixture.getString("userId", ""), repository.profile().id);
            repository.logout();
        }
        String username = "camera_" + UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        var created = repository.register(username + "@example.test", username, "Camera QA", password);
        assertTrue(fixture.edit().putString("userId", created.id).commit());
        ownsSession = true; repository.login(username + "@example.test", password);
        if (grantCamera) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), Manifest.permission.CAMERA);
        }
        shell("am start -W -n com.taktak.app/.MainActivity");
        return repository;
    }

    @Test public void deniedCameraPermissionShowsSafeMessage() throws Exception {
        assertNotEquals("Prepare denied camera permission before this case", PackageManager.PERMISSION_GRANTED,
                context.checkSelfPermission(Manifest.permission.CAMERA));
        loginAndLaunch(false);
        SessionRoutingTest.awaitView(R.id.cameraPermissionButton);
        actOnResumed(activity -> {
            assertFalse(activity.findViewById(R.id.captureButton).isEnabled());
            activity.findViewById(R.id.cameraPermissionButton).performClick();
        });
        denyPermissionDialog();
        awaitMessage(R.string.moment_camera_denied);
        assertNotEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA));
        actOnResumed(activity -> assertFalse(activity.findViewById(R.id.captureButton).isEnabled()));
        capture("camera-denied.png");
    }

    @Test public void cameraPermissionCanBeGrantedFromSystemDialog() throws Exception {
        assertNotEquals("Prepare denied permission before this case", PackageManager.PERMISSION_GRANTED,
                context.checkSelfPermission(Manifest.permission.CAMERA));
        loginAndLaunch(false);
        SessionRoutingTest.awaitView(R.id.cameraPermissionButton);
        actOnResumed(activity -> activity.findViewById(R.id.cameraPermissionButton).performClick());
        denyPermissionDialog();
        awaitMessage(R.string.moment_camera_denied);
        actOnResumed(activity -> activity.findViewById(R.id.cameraPermissionButton).performClick());
        var automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        var service = automation.getServiceInfo();
        service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        automation.setServiceInfo(service);
        AtomicBoolean allowed = new AtomicBoolean();
        await(() -> {
            if (allowed.get()) { return true; }
            var root = automation.getRootInActiveWindow();
            allowed.set(root != null && allowPermissionNode(root));
            return allowed.get();
        });
        awaitCaptureReady();
        awaitMessage(R.string.preview_capture_hint);
        actOnResumed(activity -> {
            assertFalse(activity.findViewById(R.id.cameraPermissionButton).isShown());
            assertFalse(activity.findViewById(R.id.cameraPlaceholder).isShown());
        });
        capture("camera-permission-granted.png");
    }

    private boolean allowPermissionNode(AccessibilityNodeInfo node) {
        String id = node.getViewIdResourceName();
        if (id != null && (id.endsWith(":id/permission_allow_foreground_only_button")
                || id.endsWith(":id/permission_allow_button"))) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            var child = node.getChild(i);
            if (child != null && allowPermissionNode(child)) { return true; }
        }
        return false;
    }

    @Test public void realPhotoRoundTripWithOfflineRetry() throws Exception {
        var repository = loginAndLaunch(true);
        awaitCaptureReady();
        capture("camera-preview.png");
        actOnResumed(activity -> activity.findViewById(R.id.captureButton).performClick());
        PreviewTestSupport.ready();
        boolean offline = "true".equals(InstrumentationRegistry.getArguments().getString("taktakAwaitMediaNetworkRestore"));
        if (offline) {
            android.util.Log.i("TaktakQa", "MEDIA_READY_FOR_DISCONNECT");
            // Host dừng listener smoke, không thay mạng điện thoại hoặc database test.
            long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
            while (healthy() && android.os.SystemClock.elapsedRealtime() < deadline) { android.os.SystemClock.sleep(200); }
            assertFalse("Host must disconnect test backend", healthy());
        }
        PreviewTestSupport.onPreview(activity -> PreviewTestSupport.model(activity).savePrivate());
        if (offline) {
            awaitMessage(R.string.moment_transfer_error);
            actOnResumed(activity -> assertNull(PreviewTestSupport.model((com.taktak.app.ui.camera.PreviewActivity) activity).state().getValue().downloaded()));
            capture("media-offline.png");
            android.util.Log.i("TaktakQa", "MEDIA_WAITING_FOR_RESTORE");
            long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
            while (!healthy() && android.os.SystemClock.elapsedRealtime() < deadline) { android.os.SystemClock.sleep(200); }
            assertTrue("Host must restore test backend", healthy());
            PreviewTestSupport.onPreview(activity -> PreviewTestSupport.model(activity).savePrivate());
        }
        awaitMessage(R.string.moment_downloaded);
        final byte[][] displayed = new byte[1][]; final String[] id = new String[1];
        actOnResumed(activity -> {
            var model = PreviewTestSupport.model((com.taktak.app.ui.camera.PreviewActivity) activity);
            displayed[0] = model.state().getValue().downloaded();
            try { var field = com.taktak.app.viewmodel.PreviewViewModel.class.getDeclaredField("momentId"); field.setAccessible(true); id[0] = (String) field.get(model); }
            catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            assertTrue(activity.findViewById(R.id.previewImage).isShown());
        });
        assertNotNull(displayed[0]); assertNotNull(id[0]);
        assertArrayEquals(repository.moments().download(id[0]), displayed[0]);
        Bitmap bitmap = BitmapFactory.decodeByteArray(displayed[0], 0, displayed[0].length);
        assertNotNull(bitmap); assertEquals(bitmap.getWidth(), bitmap.getHeight()); bitmap.recycle();
        capture("server-roundtrip.png");
    }

    @Test public void privateApiRejectsOtherUserAndUnauthenticatedImage() throws Exception {
        String firstName = "moment_test_" + UUID.randomUUID(); String secondName = "moment_test_" + UUID.randomUUID();
        var constructor = EncryptedSessionStore.class.getDeclaredConstructor(Context.class, String.class, String.class);
        constructor.setAccessible(true); // Chỉ androidTest; không mở rộng API production để tạo fixture.
        var firstStore = constructor.newInstance(context, BuildConfig.BASE_URL, firstName);
        var secondStore = constructor.newInstance(context, BuildConfig.BASE_URL, secondName);
        var first = new ApiClient(BuildConfig.BASE_URL, firstStore); var second = new ApiClient(BuildConfig.BASE_URL, secondStore);
        try {
            loginFixture(new AuthRepository(first)); loginFixture(new AuthRepository(second));
            Bitmap source = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888); source.eraseColor(Color.BLUE);
            byte[] jpeg = PhotoProcessor.squareJpeg(source, 90); source.recycle();
            assertTrue(jpeg.length <= 800 * 1024);
            String id = new MomentRepository(first).upload(jpeg);
            assertNotNull(new MomentRepository(first).download(id));
            var denied = second.moments.image(id).execute(); assertEquals(404, denied.code());
            if (denied.errorBody() != null) { denied.errorBody().close(); }
            var client = new okhttp3.OkHttpClient.Builder().followRedirects(false).build();
            var request = new okhttp3.Request.Builder().url(BuildConfig.BASE_URL + "api/moments/" + id + "/image").build();
            try (var absent = client.newCall(request).execute()) { assertEquals(401, absent.code()); }
            try (var forged = client.newCall(request.newBuilder().header("Authorization", "Bearer invalid.jwt.token").build()).execute()) {
                assertEquals(401, forged.code());
            }
            var oldOwnerRepository = new MomentRepository(first);
            first.sessions.logout(); loginFixture(new AuthRepository(first));
            try { oldOwnerRepository.download(id); fail("A stale media operation must not cross login sessions"); }
            catch (com.taktak.app.data.remote.ApiFailure cancelled) {
                assertEquals(com.taktak.app.data.remote.ApiFailure.Kind.CANCELLED, cancelled.kind);
            }
        } finally {
            try { first.sessions.logout(); } finally { second.sessions.logout(); }
        }
    }

    @Test public void rotationAndCropKeepCorrectOrientationAndBoundedJpeg() throws Exception {
        Bitmap image = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image); Paint paint = new Paint(); paint.setColor(Color.RED); canvas.drawRect(0, 0, 300, 300, paint);
        paint.setColor(Color.BLUE); canvas.drawRect(300, 0, 600, 300, paint);
        byte[] bytes = PhotoProcessor.squareJpeg(image, 90); image.recycle();
        Bitmap result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        assertEquals(300, result.getWidth()); assertEquals(300, result.getHeight());
        int top = result.getPixel(150, 40), bottom = result.getPixel(150, 260);
        assertTrue(Color.red(top) > Color.blue(top)); assertTrue(Color.blue(bottom) > Color.red(bottom)); result.recycle();
    }

    @Test public void cameraRedesignBusyLightDarkAndAccessibleControls() throws Exception {
        int original = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        try {
            loginAndLaunch(true);
            for (int mode : new int[] { androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES }) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode));
                awaitCaptureReady();
                String variant = mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO ? "light" : "dark";
                capture("redesign-" + variant + ".png");
                actOnResumed(activity -> {
                    View shutter = activity.findViewById(R.id.captureButton);
                    float density = activity.getResources().getDisplayMetrics().density;
                    assertTrue(shutter.getWidth() >= 48 * density);
                    assertTrue(shutter.getHeight() >= 48 * density);
                    assertEquals(context.getString(R.string.moment_capture), shutter.getContentDescription());
                    assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,
                            activity.findViewById(R.id.momentStatus).getAccessibilityLiveRegion());
                    var fragment = MainNavigationTestSupport.camera(activity);
                    var model = new androidx.lifecycle.ViewModelProvider(fragment).get(CaptureViewModel.class);
                    // Renderer-only fixture; actual upload/JWT acceptance is a separate test.
                    try {
                        var field = CaptureViewModel.class.getDeclaredField("state"); field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        var state = (androidx.lifecycle.MutableLiveData<CaptureViewModel.State>) field.get(model);
                        state.setValue(new CaptureViewModel.State(true, R.string.moment_capturing, null));
                        assertFalse(shutter.isEnabled());
                        assertTrue(activity.findViewById(R.id.momentProgress).isShown());
                        assertEquals(context.getString(R.string.camera_status_busy),
                                ((android.widget.TextView) activity.findViewById(R.id.statusTitle)).getText().toString());
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                });
                capture("redesign-" + variant + "-busy.png");
                actOnResumed(activity -> {
                    var fragment = MainNavigationTestSupport.camera(activity);
                    var model = new androidx.lifecycle.ViewModelProvider(fragment).get(CaptureViewModel.class);
                    try {
                        var field = CaptureViewModel.class.getDeclaredField("state"); field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        var state = (androidx.lifecycle.MutableLiveData<CaptureViewModel.State>) field.get(model);
                        state.setValue(new CaptureViewModel.State(false, R.string.preview_capture_hint, null));
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                });
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(original));
        }
    }

    private void loginFixture(AuthRepository repository) throws Exception {
        String name = "media_" + UUID.randomUUID().toString().replace("-", ""); String password = UUID.randomUUID().toString();
        repository.register(name + "@example.test", name, "Private media", password); repository.login(name + "@example.test", password);
    }
    private void denyPermissionDialog() {
        var automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        var service = automation.getServiceInfo(); service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        automation.setServiceInfo(service);
        long deadline = android.os.SystemClock.elapsedRealtime() + 10000;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            var root = automation.getRootInActiveWindow();
            if (root != null && denyNode(root)) { return; }
            android.os.SystemClock.sleep(100);
        }
        fail("Camera permission Deny button was not found; do not blind-tap.");
    }
    private boolean denyNode(AccessibilityNodeInfo node) {
        String id = node.getViewIdResourceName();
        String text = node.getText() == null ? "" : node.getText().toString().trim();
        if ((id != null && id.endsWith(":id/permission_deny_button")) || "Don't allow".equalsIgnoreCase(text)
                || "Không cho phép".equalsIgnoreCase(text) || "Từ chối".equalsIgnoreCase(text)) {
            var button = node;
            while (button != null && !button.isClickable()) { button = button.getParent(); }
            return button != null && button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            var child = node.getChild(i); if (child != null && denyNode(child)) { return true; }
        }
        return false;
    }
    private void awaitCaptureReady() {
        await(() -> {
            AtomicBoolean enabled = new AtomicBoolean();
            pollResumed(activity -> {
                var button = activity.findViewById(R.id.captureButton);
                var preview = (androidx.camera.view.PreviewView) activity.findViewById(R.id.cameraPreview);
                enabled.set(button != null && button.isEnabled() && preview != null
                        && preview.getPreviewStreamState().getValue() == androidx.camera.view.PreviewView.StreamState.STREAMING);
            });
            return enabled.get();
        });
    }
    private void awaitMessage(int message) {
        await(() -> {
            AtomicBoolean matches = new AtomicBoolean();
            pollResumed(activity -> {
                var text = (android.widget.TextView) activity.findViewById(activity instanceof com.taktak.app.ui.camera.PreviewActivity
                        ? R.id.previewNotice : R.id.momentStatus);
                matches.set(text != null && text.getText().toString().equals(context.getString(message)));
            });
            return matches.get();
        });
    }
    private void await(java.util.function.BooleanSupplier condition) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 40000;
        while (!condition.getAsBoolean() && android.os.SystemClock.elapsedRealtime() < deadline) { android.os.SystemClock.sleep(100); }
        assertTrue("Expected camera/media state before timeout", condition.getAsBoolean());
    }
    private void pollResumed(java.util.function.Consumer<android.app.Activity> action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (var activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) { action.accept(activity); }
        });
    }
    private void actOnResumed(java.util.function.Consumer<android.app.Activity> action) {
        AtomicBoolean performed = new AtomicBoolean();
        await(() -> {
            pollResumed(activity -> { if (!performed.get()) { action.accept(activity); performed.set(true); } });
            return performed.get();
        });
    }
    private boolean healthy() {
        java.net.HttpURLConnection connection = null;
        try {
            connection = (java.net.HttpURLConnection) new java.net.URL(BuildConfig.BASE_URL + "actuator/health").openConnection();
            connection.setConnectTimeout(700); connection.setReadTimeout(700); return connection.getResponseCode() == 200;
        } catch (IOException unavailable) { return false; }
        finally { if (connection != null) { connection.disconnect(); } }
    }
    private void shell(String command) throws Exception {
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        File directory = new File(context.getExternalFilesDir(null), "artifacts/3d"); assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); }
        finally { bitmap.recycle(); }
    }
}
