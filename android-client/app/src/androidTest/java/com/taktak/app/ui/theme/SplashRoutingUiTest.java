package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.ui.auth.LoginActivity;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** RAM-only auth fixture: không đăng xuất tài khoản người dùng hoặc gọi backend. */
@RunWith(AndroidJUnit4.class)
public class SplashRoutingUiTest {
    private MediaThemeFixture fixture;
    private File session;
    private byte[] before;
    @Before public void prepare() throws Exception {
        assertEquals("Explicit QA opt-in", "true", InstrumentationRegistry.getArguments().getString("taktak.allowSplashQa"));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        session = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        before = session.exists() ? Files.readAllBytes(session.toPath()) : null;
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        fixture = new MediaThemeFixture(context);
    }
    @After public void cleanup() throws Exception {
        main(() -> {
            var monitor = ActivityLifecycleMonitorRegistry.getInstance();
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(monitor.getActivitiesInStage(stage)))
                if (activity instanceof MainActivity || activity instanceof LoginActivity) activity.finish();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        if (fixture != null) fixture.close();
        if (session != null) assertArrayEquals("Encrypted session unchanged", before,
                session.exists() ? Files.readAllBytes(session.toPath()) : null);
    }
    @Test public void verifiedProfileEntersMainAndSurvivesRecreationAndResume() throws Exception {
        fixture.mockProfile = true;
        launch();
        await(R.id.cameraPanel, MainActivity.class);
        // Chờ ở test thread để quan sát exit, không giữ splash trong production.
        android.os.SystemClock.sleep(1000);
        main(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if (activity instanceof MainActivity) { activity.recreate(); break; }
        });
        await(R.id.cameraPanel, MainActivity.class);
        assertTrue(fixture.auth.hasSession());
        assertEquals(0, fixture.posts.get());
    }
    @Test public void storedSessionWithFailedValidationNeverOpensCamera() throws Exception {
        fixture.mockProfile = false;
        launch();
        await(R.id.retryButton, MainActivity.class);
        main(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if (activity instanceof MainActivity) assertFalse(activity.findViewById(R.id.cameraPanel).isShown());
        });
        assertTrue(fixture.auth.hasSession());
    }
    @Test public void missingSessionRoutesToLoginWithoutTrustingStoredUi() throws Exception {
        var field = AuthRepository.class.getDeclaredField("client"); field.setAccessible(true);
        ((ApiClient) field.get(fixture.auth)).sessions.clear();
        launch();
        await(R.id.emailInput, LoginActivity.class);
        assertFalse(fixture.auth.hasSession());
    }
    private void launch() throws Exception {
        // MIUI chặn startActivity từ process test nền; shell mở đúng launcher exported.
        var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            String result = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue("Exported launcher must start", result.contains("Status: ok"));
        }
    }
    private static void await(int id, Class<?> screen) throws Exception {
        long end = android.os.SystemClock.uptimeMillis() + 20000;
        while (android.os.SystemClock.uptimeMillis() < end) {
            var found = new AtomicBoolean();
            main(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    View view = activity.findViewById(id);
                    if (screen.isInstance(activity) && view != null && view.isShown()) found.set(true);
                }
            });
            if (found.get()) return;
            android.os.SystemClock.sleep(80);
        }
        fail("Expected session destination: " + screen.getSimpleName());
    }
    private static void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
}
