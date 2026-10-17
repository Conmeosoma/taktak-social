package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.color.MaterialColors;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.RefreshRequest;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.data.session.Session;
import com.taktak.app.viewmodel.SessionViewModel;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Fixture sở hữu phiên riêng; chỉ kiểm tra renderer/lifecycle và API thật của phiên QA. */
@RunWith(AndroidJUnit4.class)
public class CameraM3Test {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa_m3_camera", Context.MODE_PRIVATE);
    private boolean ownsSession;

    @Before public void preservePersonalSession() throws Exception {
        var store = new EncryptedSessionStore(context, BuildConfig.BASE_URL);
        if (store.load() != null) {
            String owner = fixture.getString("owner", "");
            assertFalse("Refusing to replace a personal session", owner.isEmpty());
            assertEquals("QA owner must match", owner, AuthGraph.repository(context).profile().id);
            AuthGraph.repository(context).logout();
        }
        assertTrue(fixture.edit().clear().commit());
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }
    @After public void cleanupOwnedFixture() throws Exception {
        if (ownsSession) {
            try { AuthGraph.repository(context).logout(); }
            finally { assertTrue(fixture.edit().clear().commit()); }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity activity : new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))) { activity.finish(); }
        });
    }

    @Test public void cameraAndSessionLayoutsFitInsideFourTabShell() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                for (int[] size : new int[][]{{280, 560}, {360, 800}, {840, 420}}) {
                    var config = new Configuration(context.getResources().getConfiguration()); config.fontScale = 1.5f;
                    config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                    var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_TakTak_Camera);
                    float density = themed.getResources().getDisplayMetrics().density;
                    View root = LayoutInflater.from(themed).inflate(R.layout.activity_main, null, false);
                    assertEquals("Main shell gets Expressive palette", themed.getColor(R.color.m3_primary),
                            MaterialColors.getColor(root.getContext(), androidx.appcompat.R.attr.colorPrimary, 0));
                    View page = LayoutInflater.from(root.getContext()).inflate(R.layout.fragment_camera_page, null, false);
                    // FragmentContainerView chỉ nhận View thuộc Fragment; dùng slot cùng context trong đo layout tách rời.
                    View slot = page.findViewById(R.id.cameraContent);
                    var parent = (ViewGroup) slot.getParent(); int index = parent.indexOfChild(slot);
                    var container = new android.widget.FrameLayout(slot.getContext());
                    container.setLayoutParams(slot.getLayoutParams());
                    parent.removeView(slot); parent.addView(container, index);
                    View camera = LayoutInflater.from(container.getContext()).inflate(R.layout.fragment_camera, container, false);
                    container.addView(camera);
                    camera.findViewById(R.id.cameraPermissionButton).setVisibility(View.GONE);
                    camera.findViewById(R.id.cameraPlaceholder).setVisibility(View.GONE);
                    ((TextView) camera.findViewById(R.id.momentStatus)).setText(R.string.moment_transfer_error);
                    camera.findViewById(R.id.momentRetryButton).setVisibility(View.VISIBLE);
                    measure(page, size, density); bounds(page, page.getWidth());
                    assertEquals("Camera gets Expressive palette", themed.getColor(R.color.m3_primary),
                            MaterialColors.getColor(camera.getContext(), androidx.appcompat.R.attr.colorPrimary, 0));
                    assertTrue(page.findViewById(R.id.cameraPage).getWidth() <= Math.round(480 * density));
                    View preview = camera.findViewById(R.id.cameraPreview), shutter = camera.findViewById(R.id.captureButton);
                    assertTrue(Math.abs(preview.getWidth() - preview.getHeight()) <= 1);
                    assertEquals(shutter.getWidth(), shutter.getHeight()); assertTrue(shutter.getHeight() >= Math.round(48 * density));
                    root.findViewById(R.id.retryButton).setVisibility(View.VISIBLE);
                    ((TextView) root.findViewById(R.id.sessionMessage)).setText(R.string.auth_network_error);
                    measure(root, size, density); bounds(root, root.getWidth());
                    assertTrue(root.findViewById(R.id.retryButton).getHeight() >= Math.round(56 * density));
                    for (int[] pair : new int[][]{{R.color.m3_on_camera_shutter, R.color.m3_camera_shutter},
                            {R.color.m3_on_error_container, R.color.m3_error_container},
                            {R.color.m3_on_success_container, R.color.m3_success_container},
                            {R.color.m3_on_secondary_container, R.color.m3_secondary_container}}) {
                        assertTrue("Status/shutter contrast >=4.5", ColorUtils.calculateContrast(themed.getColor(pair[0]), themed.getColor(pair[1])) >= 4.5);
                    }
                }
            }
        });
    }

    @Test public void sessionCheckingPermissionAndOfflineRetryKeepPrivateContentHidden() throws Exception {
        Assume.assumeTrue("Requires host-managed connector", "true".equals(InstrumentationRegistry.getArguments().getString("taktakSessionOffline")));
        login(); launch(); SessionRoutingTest.awaitView(R.id.cameraTitle);
        onMain(activity -> {
            var model = new androidx.lifecycle.ViewModelProvider(activity).get(SessionViewModel.class);
            try {
                var field = SessionViewModel.class.getDeclaredField("state"); field.setAccessible(true);
                @SuppressWarnings("unchecked") var state = (androidx.lifecycle.MutableLiveData<SessionViewModel.State>) field.get(model);
                state.setValue(new SessionViewModel.State(SessionViewModel.Destination.CHECKING, R.string.session_checking));
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            assertFalse(activity.findViewById(R.id.cameraPanel).isShown());
            assertTrue(activity.findViewById(R.id.sessionProgress).isShown());
        });
        capture("session-checking.png");
        onMain(activity -> new androidx.lifecycle.ViewModelProvider(activity).get(SessionViewModel.class).check(false));
        awaitText(R.id.sessionMessage, R.string.auth_permission_denied);
        onMain(activity -> {
            assertTrue(activity.findViewById(R.id.sessionCard).isShown());
            assertFalse(activity.findViewById(R.id.cameraPanel).isShown());
        });
        capture("session-permission.png");
        onMain(activity -> activity.findViewById(R.id.retryButton).performClick());
        SessionRoutingTest.awaitView(R.id.cameraTitle);
        android.util.Log.i("TaktakQa", "M3_SESSION_READY_OFFLINE_" + InstrumentationRegistry.getArguments().getString("taktakQaRun", "default"));
        await(() -> !healthy(), "Host disconnects QA backend");
        onMain(activity -> new androidx.lifecycle.ViewModelProvider(activity).get(SessionViewModel.class).check(true));
        awaitText(R.id.sessionMessage, R.string.auth_network_error);
        assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        onMain(activity -> {
            assertFalse(activity.findViewById(R.id.cameraPanel).isShown());
            assertTrue(activity.findViewById(R.id.retryButton).isShown());
        });
        capture("session-offline.png");
        await(this::healthy, "Host reconnects QA backend");
        onMain(activity -> activity.findViewById(R.id.retryButton).performClick());
        SessionRoutingTest.awaitView(R.id.cameraTitle); capture("session-recovered.png");
        onMain(activity -> activity.findViewById(R.id.logoutButton).performClick());
        awaitText(R.id.formNotice, R.string.session_logged_out);
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load()); capture("session-logout.png");
    }

    @Test public void revokedRefreshReturnsToLoginWithoutCamera() throws Exception {
        login();
        var store = new EncryptedSessionStore(context, BuildConfig.BASE_URL); Session previous = store.load(); assertNotNull(previous);
        assertEquals(204, new ApiClient(BuildConfig.BASE_URL, store).auth.logout(new RefreshRequest(previous.refreshToken)).execute().code());
        store.save(new Session("invalid.jwt.token", previous.refreshToken, 0, previous.generation));
        var field = AuthGraph.class.getDeclaredField("repository"); field.setAccessible(true); field.set(null, null);
        launch(); awaitText(R.id.formNotice, R.string.auth_invalid_credentials);
        assertNull(store.load()); capture("expired-session.png");
    }

    private void login() throws Exception {
        String username = "m3camera_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
        var repository = AuthGraph.repository(context);
        var created = repository.register(username + "@example.test", username, "M3 Camera QA", password);
        assertTrue(fixture.edit().putString("owner", created.id).commit()); ownsSession = true;
        repository.login(username + "@example.test", password);
    }
    private void launch() throws Exception {
        var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             var reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder result = new StringBuilder(); String line; while ((line = reader.readLine()) != null) { result.append(line); }
            assertTrue(result.toString().contains("Status: ok"));
        }
    }
    private void onMain(java.util.function.Consumer<MainActivity> action) {
        SessionRoutingTest.onResumed(activity -> { assertTrue(activity instanceof MainActivity); action.accept((MainActivity) activity); });
    }
    private void awaitText(int id, int resource) {
        await(() -> {
            AtomicBoolean found = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    TextView text = activity.findViewById(id);
                    if (text != null && text.isShown() && text.getText().toString().equals(activity.getString(resource))) { found.set(true); }
                }
            }); return found.get();
        }, "Expected session UI state");
    }
    private void await(java.util.function.BooleanSupplier condition, String message) {
        long deadline = SystemClock.elapsedRealtime() + 45000;
        while (SystemClock.elapsedRealtime() < deadline) { if (condition.getAsBoolean()) { return; } SystemClock.sleep(100); }
        fail(message);
    }
    private boolean healthy() {
        java.net.HttpURLConnection connection = null;
        try {
            connection = (java.net.HttpURLConnection) new java.net.URL(BuildConfig.BASE_URL + "actuator/health").openConnection();
            connection.setConnectTimeout(500); connection.setReadTimeout(500); return connection.getResponseCode() == 200;
        } catch (IOException unavailable) { return false; }
        finally { if (connection != null) { connection.disconnect(); } }
    }
    private void measure(View root, int[] size, float density) {
        root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(size[0] * density), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.round(size[1] * density), View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }
    private void bounds(View view, int parentWidth) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if (view instanceof TextView text && text.getLayout() != null) {
            assertTrue("No clipped text: " + view.getClass().getSimpleName(), text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) { for (int i = 0; i < group.getChildCount(); i++) { bounds(group.getChildAt(i), group.getWidth()); } }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(1000); var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/m3e-camera"); assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)); }
        finally { bitmap.recycle(); }
    }
}
