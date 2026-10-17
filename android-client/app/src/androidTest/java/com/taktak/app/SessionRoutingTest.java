package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.model.AuthModels.RefreshRequest;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.data.session.Session;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionRoutingTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Before public void prepare() throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        AuthGraph.repository(context).logout();
        replaceGraph(null);
    }
    @After public void cleanup() throws Exception {
        // Chỉ chạy suite này trên AVD test riêng: không reset app data/keystore.
        if ("true".equals(InstrumentationRegistry.getArguments().getString("taktakKeepSession"))) { return; }
        try { AuthGraph.repository(context).logout(); } finally { replaceGraph(null); }
    }

    private Session login() throws Exception {
        String username = "route_" + UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        AuthRepository repository = AuthGraph.repository(context);
        repository.register(username + "@example.test", username, "Routing test", password);
        repository.login(username + "@example.test", password);
        return new EncryptedSessionStore(context, BuildConfig.BASE_URL).load();
    }

    @Test public void missingSessionGoesToLoginAsTaskRoot() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitView(R.id.emailInput);
            onResumed(activity -> assertTrue(activity.isTaskRoot()));
        }
    }

    @Test public void validStoredSessionEntersCamera() throws Exception {
        login();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitView(R.id.cameraTitle);
        }
    }

    @Test public void storageClearFailureBlocksNavigationUntilLogoutRetrySucceeds() throws Exception {
        login();
        var encrypted = new EncryptedSessionStore(context, BuildConfig.BASE_URL);
        AtomicBoolean failClear = new AtomicBoolean(true);
        var failingStore = new com.taktak.app.data.session.SessionStore() {
            public Session load() throws java.io.IOException { return encrypted.load(); }
            public void save(Session session) throws java.io.IOException { encrypted.save(session); }
            public void clear() throws java.io.IOException {
                if (failClear.get()) { throw new java.io.IOException("Fixture storage unavailable."); }
                encrypted.clear();
            }
        };
        replaceGraph(new AuthRepository(new ApiClient(BuildConfig.BASE_URL, failingStore)));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitView(R.id.cameraTitle);
            onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
            awaitText(R.id.sessionMessage, R.string.auth_storage_error);
            assertNotNull(encrypted.load());
            onResumed(activity -> activity.findViewById(R.id.retryButton).performClick());
            awaitText(R.id.sessionMessage, R.string.auth_storage_error);
            scenario.recreate();
            awaitText(R.id.sessionMessage, R.string.auth_storage_error);
            failClear.set(false);
            onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
            awaitText(R.id.formNotice, R.string.session_logged_out);
            assertNull(encrypted.load());
        } finally { failClear.set(false); }
    }

    @Test public void localNetworkPermissionGateDoesNotDiscardSessionAndRetryRecovers() throws Exception {
        login();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitView(R.id.cameraTitle);
            onResumed(activity -> new androidx.lifecycle.ViewModelProvider((MainActivity) activity)
                    .get(com.taktak.app.viewmodel.SessionViewModel.class).check(false));
            awaitText(R.id.sessionMessage, R.string.auth_permission_denied);
            onResumed(activity -> assertFalse(activity.findViewById(R.id.cameraPanel).isShown()));
            assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
            onResumed(activity -> activity.findViewById(R.id.retryButton).performClick());
            awaitView(R.id.cameraTitle);
        }
    }

    @Test public void readTimeoutShowsNetworkErrorWithoutOpeningCameraOrDeletingSession() throws Exception {
        login();
        try (var server = new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            Thread stalled = new Thread(() -> {
                try (var socket = server.accept()) {
                    // Kết nối thật nhưng không trả response: OkHttp phải chạm read timeout.
                    Thread.sleep(20000);
                } catch (java.io.IOException ignored) {
                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            stalled.setDaemon(true);
            stalled.start();
            replaceGraph(new AuthRepository(new ApiClient("http://127.0.0.1:" + server.getLocalPort() + "/",
                    new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                awaitText(R.id.sessionMessage, R.string.auth_network_error);
                onResumed(activity -> assertFalse(activity.findViewById(R.id.cameraPanel).isShown()));
                assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
            } finally {
                stalled.interrupt();
                stalled.join(1000);
                replaceGraph(new AuthRepository(new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
            }
        }
    }

    @Test public void diskRestoreRecreationAndLogoutClearSessionAndBackStack() throws Exception {
        Session old = login();
        replaceGraph(null); // Mở lại graph/store từ disk; cold process được kiểm tra riêng bằng ADB.
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitView(R.id.cameraTitle);
            scenario.recreate();
            awaitView(R.id.cameraTitle);
            capture("camera.png");
            onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
            awaitText(R.id.formNotice, R.string.session_logged_out);
            capture("logout.png");
            onResumed(activity -> assertTrue(activity.isTaskRoot()));
            assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
            var raw = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
            var replay = raw.auth.refresh(new RefreshRequest(old.refreshToken)).execute();
            assertEquals(401, replay.code());
            if (replay.errorBody() != null) { replay.errorBody().close(); }
        }
    }

    @Test public void revokedRefreshRoutesToLoginWithoutCameraOrLoop() throws Exception {
        Session old = login();
        ApiClient raw = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
        assertTrue(raw.auth.logout(new RefreshRequest(old.refreshToken)).execute().isSuccessful());
        new EncryptedSessionStore(context, BuildConfig.BASE_URL).save(new Session("invalid.jwt.token", old.refreshToken, 0, 0));
        replaceGraph(null);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitText(R.id.formNotice, R.string.auth_invalid_credentials);
            assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        }
    }

    @Test public void unavailableBackendShowsRetryKeepsSessionAndAllowsLocalLogout() throws Exception {
        login();
        // Transport lỗi thật tới loopback đóng, không đổi BASE_URL production hoặc tắt mạng người dùng.
        replaceGraph(new AuthRepository(new ApiClient("http://127.0.0.1:1/", new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitText(R.id.sessionMessage, R.string.auth_network_error);
            awaitView(R.id.retryButton);
            assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
            onResumed(activity -> assertFalse(activity.findViewById(R.id.cameraPanel).isShown()));
            capture("network-error.png");
            onResumed(activity -> activity.findViewById(R.id.retryButton).performClick());
            awaitText(R.id.sessionMessage, R.string.auth_network_error);
            replaceGraph(new AuthRepository(new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
            onResumed(activity -> activity.findViewById(R.id.retryButton).performClick());
            awaitView(R.id.cameraTitle);
            replaceGraph(new AuthRepository(new ApiClient("http://127.0.0.1:1/", new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
            onResumed(activity -> new androidx.lifecycle.ViewModelProvider((MainActivity) activity)
                    .get(com.taktak.app.viewmodel.SessionViewModel.class).check(true));
            awaitText(R.id.sessionMessage, R.string.auth_network_error);
            onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
            awaitText(R.id.formNotice, R.string.session_logout_local_only);
            assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        }
    }

    private static void replaceGraph(AuthRepository repository) throws Exception {
        // Fixture instrumentation, không thêm setter/test injection vào production graph.
        var field = AuthGraph.class.getDeclaredField("repository");
        field.setAccessible(true);
        field.set(null, repository);
    }

    static void onResumed(java.util.function.Consumer<Activity> action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            assertEquals(1, activities.size());
            action.accept(activities.iterator().next());
        });
    }
    static void awaitView(int id) { await(id, 0); }
    private static void awaitText(int id, int text) { await(id, text); }
    private static void await(int id, int text) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 35000;
        AtomicBoolean found = new AtomicBoolean();
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    View view = activity.findViewById(id);
                    if (view != null && view.isShown() && (text == 0 || ((TextView) view).getText().toString().equals(activity.getString(text)))) {
                        found.set(true);
                    }
                }
            });
            if (found.get()) { return; }
            android.os.SystemClock.sleep(50);
        }
        fail("Expected session UI was not reached: " + id);
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        var directory = new java.io.File(context.getExternalFilesDir(null), "artifacts/3c");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
