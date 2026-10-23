package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
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
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BrandedIntroUiTest {
    private Context context;
    private MediaThemeFixture fixture;
    private android.content.SharedPreferences preferences;
    private Map<String, ?> original;
    private File session;
    private byte[] sessionBefore;
    private boolean launched;
    @Before public void setup() throws Exception {
        assertEquals("Explicit QA opt-in", "true", InstrumentationRegistry.getArguments().getString("taktak.allowIntroQa"));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        session = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        sessionBefore = session.exists() ? Files.readAllBytes(session.toPath()) : null;
        preferences = context.getSharedPreferences(IntroPreferences.FILE, Context.MODE_PRIVATE);
        original = new HashMap<>(preferences.getAll());
        assertTrue(preferences.edit().putBoolean("enabled", false).putBoolean("sound", false).commit());
        fixture = new MediaThemeFixture(context); fixture.mockProfile = true;
    }
    @After public void cleanup() throws Exception {
        main(() -> {
            var monitor = ActivityLifecycleMonitorRegistry.getInstance();
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(monitor.getActivitiesInStage(stage)))
                if (activity instanceof MainActivity) activity.finish();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        if (fixture != null) fixture.close();
        if (original != null) {
            var editor = preferences.edit().clear();
            for (var entry : original.entrySet()) editor.putBoolean(entry.getKey(), (Boolean) entry.getValue());
            assertTrue(editor.commit());
            assertEquals(original, preferences.getAll());
        }
        if (session != null) assertArrayEquals("Personal session unchanged", sessionBefore,
                session.exists() ? Files.readAllBytes(session.toPath()) : null);
    }
    @Test public void originalBundledVideoHasAudioAndExpectedTimeline() throws Exception {
        try (var descriptor = context.getResources().openRawResourceFd(R.raw.taktak_intro);
             var metadata = new MediaMetadataRetriever()) {
            metadata.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            assertEquals("720", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            assertEquals("1280", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
            long duration = Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            assertTrue(duration >= 3300 && duration <= 3400);
        }
    }
    @Test public void mutedPreviewPlaysToCompletionAndReleases() throws Exception {
        MainActivity activity = launch();
        main(activity::showIntroPreview);
        BrandedIntro player = awaitPlaying(activity);
        assertFalse(player.isSoundAllowed());
        await(() -> intro(activity) == null, "Original video completes");
        main(() -> assertFalse(player.isPlaying()));
        assertNull(field(player, "player")); assertNull(field(player, "surface"));
    }
    @Test public void skipStopsSinglePlayerAndSurfaceImmediately() throws Exception {
        MainActivity activity = launch();
        main(activity::showIntroPreview);
        BrandedIntro player = awaitPlaying(activity);
        main(() -> {
            activity.showIntroPreview();
            assertSame(player, intro(activity));
            activity.findViewById(R.id.introSkip).performClick();
            assertNull(intro(activity));
            assertFalse(player.isPlaying());
            player.finish();
        });
        assertNull(field(player, "player")); assertNull(field(player, "surface"));
    }
    @Test public void backgroundAndRecreationDoNotReplay() throws Exception {
        MainActivity activity = launch();
        main(activity::showIntroPreview); BrandedIntro player = awaitPlaying(activity);
        main(() -> activity.moveTaskToBack(true));
        await(() -> intro(activity) == null, "Pause stops playback");
        assertNull(field(player, "player"));
        MainActivity resumed = launch();
        assertSame(activity, resumed); assertNull(intro(resumed));
        main(resumed::recreate);
        await(() -> resumedMain() != null && resumedMain() != resumed, "Recreated Main");
        assertNull(intro(resumedMain()));
    }
    @Test public void settingsPersistAndPreviewCanAlwaysBeStopped() throws Exception {
        MainActivity activity = launch();
        main(() -> IntroSettings.show(activity));
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.introEnabled))
                .perform(androidx.test.espresso.action.ViewActions.click());
        assertTrue(new IntroPreferences(context).enabled());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.introPreview))
                .perform(androidx.test.espresso.action.ViewActions.click());
        awaitPlaying(activity);
        main(() -> activity.findViewById(R.id.introSkip).performClick());
        assertNull(intro(activity));
    }
    @Test public void soundOnUsesNativePlayerAndLowMemoryReleasesIt() throws Exception {
        new IntroPreferences(context).sound(true);
        MainActivity activity = launch();
        main(activity::showIntroPreview);
        BrandedIntro player = awaitPlaying(activity);
        var status = new android.os.Bundle();
        status.putString("intro_audio", player.isSoundAllowed() ? "AUDIBLE_POLICY_ALLOWED" : "MUTED_BY_DEVICE_POLICY");
        var audio = context.getSystemService(android.media.AudioManager.class);
        status.putInt("ringer", audio.getRingerMode());
        status.putInt("media_volume", audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
        status.putInt("dnd_filter", context.getSystemService(android.app.NotificationManager.class).getCurrentInterruptionFilter());
        InstrumentationRegistry.getInstrumentation().sendStatus(2, status);
        // Cửa sổ nghe QA trên test thread; không sửa timeline hoặc delay production.
        android.os.SystemClock.sleep(1100);
        main(() -> activity.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW));
        assertNull(intro(activity));
        assertNull(field(player, "player")); assertNull(field(player, "surface"));
    }
    private MainActivity launch() throws Exception {
        var fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "am start -W " + (launched ? "" : "-f 0x10008000 ")
                        + "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.taktak.app/.MainActivity");
        launched = true;
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            assertTrue(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("Status: ok"));
        }
        await(() -> resumedMain() != null && visible(resumedMain(), R.id.cameraPanel), "Validated QA Main");
        return resumedMain();
    }
    private static MainActivity resumedMain() {
        var result = new AtomicReference<MainActivity>();
        main(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                if (activity instanceof MainActivity) result.set((MainActivity) activity);
        }); return result.get();
    }
    private static Object field(Object target, String name) {
        try { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static BrandedIntro intro(MainActivity activity) { return (BrandedIntro) field(activity, "intro"); }
    private static boolean visible(MainActivity activity, int id) {
        var result = new java.util.concurrent.atomic.AtomicBoolean();
        main(() -> { View view = activity.findViewById(id); result.set(view != null && view.isShown()); }); return result.get();
    }
    private static BrandedIntro awaitPlaying(MainActivity activity) throws Exception {
        await(() -> {
            var playing = new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> playing.set(intro(activity) != null && intro(activity).isPlaying())); return playing.get();
        }, "Native player starts"); return intro(activity);
    }
    private interface Condition { boolean get() throws Exception; }
    private static void await(Condition condition, String message) throws Exception {
        long end = android.os.SystemClock.uptimeMillis() + 16000;
        while (android.os.SystemClock.uptimeMillis() < end) {
            if (condition.get()) return;
            android.os.SystemClock.sleep(50);
        } fail(message);
    }
    private static void main(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) action.run();
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
}
