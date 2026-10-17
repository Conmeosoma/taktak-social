package com.taktak.app;

import android.content.Context;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.RefreshRequest;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.session.EncryptedSessionStore;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Nghiệm thu qua launcher exported, không phụ thuộc quyền mở activity ngầm của OEM. */
@RunWith(AndroidJUnit4.class)
public class LauncherAuthAcceptanceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa3c_fixture", Context.MODE_PRIVATE);

    @Before public void preservePersonalSession() throws Exception {
        boolean existing = new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null;
        assertTrue("Refusing to replace a session not owned by this QA fixture", !existing || fixture.getBoolean("owned", false));
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }

    @After public void cleanupOwnFixture() throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("taktakKeepSession")) && fixture.getBoolean("owned", false)) {
            try { AuthGraph.repository(context).logout(); }
            finally { assertTrue(fixture.edit().remove("owned").commit()); }
        }
    }

    @Test public void realLoginFromLauncherAndEncryptedSession() throws Exception {
        var repository = AuthGraph.repository(context);
        if (fixture.getBoolean("owned", false)) { repository.logout(); }
        String username = "phone_" + UUID.randomUUID().toString().replace("-", "");
        String email = username + "@example.test";
        String password = UUID.randomUUID().toString();
        repository.register(email, username, "USB QA", password);
        launch();
        SessionRoutingTest.awaitView(R.id.emailInput);
        submit(email, UUID.randomUUID().toString());
        awaitText(R.id.formNotice, R.string.auth_invalid_credentials);
        capture("wrong-password.png");
        assertTrue(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() == null);
        assertTrue(fixture.edit().putBoolean("owned", true).commit());
        submit(email, password);
        SessionRoutingTest.awaitView(R.id.cameraTitle);
        assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        capture("signed-in.png");
    }

    @Test public void restoredLauncherRetryAndLogoutRevokesRefresh() throws Exception {
        var store = new EncryptedSessionStore(context, BuildConfig.BASE_URL);
        if (store.load() == null) {
            String username = "restore_" + UUID.randomUUID().toString().replace("-", "");
            String password = UUID.randomUUID().toString();
            var repository = AuthGraph.repository(context);
            repository.register(username + "@example.test", username, "USB restore", password);
            assertTrue(fixture.edit().putBoolean("owned", true).commit());
            repository.login(username + "@example.test", password);
        }
        var previous = store.load();
        assertNotNull(previous);
        launch();
        if ("true".equals(InstrumentationRegistry.getArguments().getString("taktakAwaitUsbRestore"))) {
            awaitText(R.id.sessionMessage, R.string.auth_network_error);
            SessionRoutingTest.onResumed(activity -> assertFalse(activity.findViewById(R.id.cameraPanel).isShown()));
            assertNotNull(store.load());
            capture("usb-disconnected.png");
            android.util.Log.i("TaktakQa", "WAITING_FOR_USB_RESTORE");
            long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
            boolean connected = false;
            while (!connected && android.os.SystemClock.elapsedRealtime() < deadline) {
                java.net.HttpURLConnection health = (java.net.HttpURLConnection) new java.net.URL(BuildConfig.BASE_URL + "actuator/health").openConnection();
                health.setConnectTimeout(1000); health.setReadTimeout(1000);
                try { connected = health.getResponseCode() == 200; }
                catch (java.io.IOException unavailable) { android.os.SystemClock.sleep(200); }
                finally { health.disconnect(); }
            }
            assertTrue("USB backend connection must be restored by host driver", connected);
            SessionRoutingTest.onResumed(activity -> activity.findViewById(R.id.retryButton).performClick());
        }
        SessionRoutingTest.awaitView(R.id.cameraTitle);
        capture("restored.png");
        SessionRoutingTest.onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
        awaitText(R.id.formNotice, R.string.session_logged_out);
        SessionRoutingTest.onResumed(activity -> assertTrue(activity.isTaskRoot()));
        assertTrue(store.load() == null);
        capture("logged-out.png");
        var raw = new ApiClient(BuildConfig.BASE_URL, store);
        var replay = raw.auth.refresh(new RefreshRequest(previous.refreshToken)).execute();
        assertEquals(401, replay.code());
        if (replay.errorBody() != null) { replay.errorBody().close(); }
        assertTrue(fixture.edit().remove("owned").commit());
    }

    private void launch() throws Exception {
        var command = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(command);
             var reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder result = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) { result.append(line); }
            assertTrue("Exported launcher must start normally", result.toString().contains("Status: ok"));
        }
    }

    private void submit(String email, String password) {
        SessionRoutingTest.onResumed(activity -> {
            ((EditText) activity.findViewById(R.id.emailInput)).setText(email);
            ((EditText) activity.findViewById(R.id.passwordInput)).setText(password);
            activity.findViewById(R.id.submitButton).performClick();
        });
    }

    private void awaitText(int id, int text) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 35000;
        AtomicBoolean matches = new AtomicBoolean();
        while (!matches.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (var activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    android.widget.TextView view = activity.findViewById(id);
                    if (view != null && view.isShown() && view.getText().toString().equals(activity.getString(text))) { matches.set(true); }
                }
            });
            if (!matches.get()) { android.os.SystemClock.sleep(50); }
        }
        assertTrue("Expected authentication UI message", matches.get());
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        var directory = new java.io.File(context.getExternalFilesDir(null), "artifacts/3c-physical");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
