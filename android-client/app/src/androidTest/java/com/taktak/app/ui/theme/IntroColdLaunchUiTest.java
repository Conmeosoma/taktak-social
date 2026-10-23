package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.ui.splash.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chạy riêng trong process instrumentation mới để xác minh startup gate thực. */
@RunWith(AndroidJUnit4.class)
public class IntroColdLaunchUiTest {
    @Test public void freshLauncherPlaysOriginalThenReturnsWithoutReplay() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        assertEquals("true", InstrumentationRegistry.getArguments().getString("taktak.allowIntroQa"));
        Context context = instrumentation.getTargetContext();
        var prefs = context.getSharedPreferences(IntroPreferences.FILE, Context.MODE_PRIVATE);
        Map<String, ?> before = new HashMap<>(prefs.getAll());
        File session = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        byte[] bytes = session.exists() ? Files.readAllBytes(session.toPath()) : null;
        try (var fixture = new MediaThemeFixture(context)) {
            fixture.mockProfile = true;
            assertTrue(prefs.edit().putBoolean("enabled", true).putBoolean("sound", true).commit());
            shell("am start -W -f 0x10008000 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.taktak.app/.MainActivity");
            long until = android.os.SystemClock.uptimeMillis() + 12000;
            var active = new AtomicReference<BrandedIntro>();
            while (active.get() == null && android.os.SystemClock.uptimeMillis() < until) {
                instrumentation.runOnMainSync(() -> {
                    MainActivity main = resumed();
                    if (main == null) return;
                    var intro = intro(main);
                    if (intro != null && intro.isPlaying()) {
                        assertFalse(main.findViewById(R.id.mainSafeContent).isShown());
                        active.set(intro);
                    }
                });
                android.os.SystemClock.sleep(25);
            }
            assertNotNull("Fresh launcher must play full intro", active.get());
            Bundle status = new Bundle();
            status.putString("intro_audio", active.get().isSoundAllowed() ? "AUDIBLE_POLICY_ALLOWED" : "MUTED_BY_DEVICE_POLICY");
            var audio = context.getSystemService(android.media.AudioManager.class);
            status.putInt("ringer", audio.getRingerMode());
            status.putInt("media_volume", audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
            status.putInt("dnd_filter", context.getSystemService(android.app.NotificationManager.class).getCurrentInterruptionFilter());
            instrumentation.sendStatus(2, status);
            long completionDeadline = android.os.SystemClock.uptimeMillis() + 10000;
            var ended = new java.util.concurrent.atomic.AtomicBoolean();
            while (!ended.get() && android.os.SystemClock.uptimeMillis() < completionDeadline) {
                instrumentation.runOnMainSync(() -> ended.set(resumed() != null && intro(resumed()) == null));
                android.os.SystemClock.sleep(40);
            }
            assertTrue("Native completion callback must release intro", ended.get());
            instrumentation.runOnMainSync(() -> {
                assertNotNull(resumed()); assertNull(intro(resumed()));
                assertTrue(resumed().findViewById(R.id.cameraPanel).isShown());
                assertFalse(active.get().isPlaying());
                resumed().moveTaskToBack(true);
            });
            shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.taktak.app/.MainActivity");
            instrumentation.runOnMainSync(() -> { assertNotNull(resumed()); assertNull(intro(resumed())); });
            assertEquals(0, fixture.posts.get());
        } finally {
            instrumentation.runOnMainSync(() -> {
                for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))
                    if (activity instanceof MainActivity) activity.finish();
            });
            instrumentation.waitForIdleSync();
            var edit = prefs.edit().clear();
            for (var entry : before.entrySet()) edit.putBoolean(entry.getKey(), (Boolean) entry.getValue());
            assertTrue(edit.commit());
            assertEquals(before, prefs.getAll());
            assertArrayEquals(bytes, session.exists() ? Files.readAllBytes(session.toPath()) : null);
        }
    }
    private static MainActivity resumed() {
        for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
            if (activity instanceof MainActivity) return (MainActivity) activity;
        return null;
    }
    private static BrandedIntro intro(MainActivity activity) {
        try { var field = MainActivity.class.getDeclaredField("intro"); field.setAccessible(true); return (BrandedIntro) field.get(activity); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void shell(String command) throws Exception {
        var fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            assertTrue(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("Status: ok"));
        }
    }
}
