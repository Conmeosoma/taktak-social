package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.R;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.ui.chat.ChatActivity;
import com.taktak.app.ui.history.MomentViewerActivity;
import com.taktak.app.viewmodel.ChatViewModel;
import com.taktak.app.viewmodel.MomentReplyViewModel;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Fixture RAM: không gửi bình luận/ảnh, không sửa tài khoản hoặc backend thật. */
@RunWith(AndroidJUnit4.class)
public class MomentReplyUiTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(180);
    private Context context;
    private ChatThemeFixture fixture;
    private Map<String, ?> appearance;
    private File sessionFile;
    private byte[] sessionBefore;
    private boolean usbQa;
    @Before public void prepare() throws Exception {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("taktak.allowReplyQa"));
        usbQa = "true".equals(InstrumentationRegistry.getArguments().getString("taktak.allowReplyUsbQa"));
        assertTrue("Device requires explicit USB QA opt-in", usbQa
                || android.os.Build.HARDWARE.contains("ranchu") || android.os.Build.HARDWARE.contains("goldfish"));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionFile = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        sessionBefore = sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null;
        appearance = new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME, 0).getAll());
        fixture = new ChatThemeFixture(context); fixture.profileEnabled = true; closeScreens(true);
        if (usbQa) launchQaHost();
    }
    @After public void cleanup() throws Exception {
        try { closeScreens(true); } finally { if (fixture != null) fixture.close(); }
        if (appearance != null) {
            var edit = context.getSharedPreferences(ThemePreferences.FILE_NAME, 0).edit();
            for (String key : new String[]{ThemePreferences.STYLE, ThemePreferences.MODE, ThemePreferences.SCHEMA, ThemePreferences.REVISION}) {
                Object value = appearance.get(key);
                if (!appearance.containsKey(key)) edit.remove(key);
                else if (value instanceof String text) edit.putString(key, text);
                else if (value instanceof Integer number) edit.putInt(key, number);
                else if (value instanceof Long number) edit.putLong(key, number);
                else if (value instanceof Boolean flag) edit.putBoolean(key, flag);
                else if (value instanceof Float number) edit.putFloat(key, number);
                else throw new AssertionError("Unexpected appearance value");
            }
            assertTrue(edit.commit());
            assertEquals("Appearance restored", appearance, context.getSharedPreferences(ThemePreferences.FILE_NAME, 0).getAll());
        }
        if (sessionFile != null) assertTrue("Encrypted personal session unchanged",
                java.util.Arrays.equals(sessionBefore, sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null));
    }
    private Intent viewer(boolean own) {
        return new Intent(context, MomentViewerActivity.class)
                .putExtra(MomentViewerActivity.EXTRA_MOMENT_ID, ChatThemeFixture.MOMENT)
                .putExtra(MomentViewerActivity.EXTRA_SENDER_ID, own ? ChatThemeFixture.SELF : ChatThemeFixture.PEER)
                .putExtra(MomentViewerActivity.EXTRA_SENDER_NAME, "QA Photo Owner")
                .putExtra(MomentViewerActivity.EXTRA_SENDER_USERNAME, "qa_photo_owner")
                .putExtra(MomentViewerActivity.EXTRA_IS_SENDER, own);
    }
    private void launchQaHost() throws Exception {
        // MIUI chặn ActivityScenario cold-start từ nền. Graph đã cô lập trước shell launch;
        // fixture chặn restore Main ở profile 503, không đăng ký FCM hoặc dùng phiên thật.
        String result;
        try (var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "am start -W -f 0x14000000 -n com.taktak.app/.MainActivity");
             var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             var output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            result = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
        assertTrue("QA host shell launch must succeed", result.matches("(?s).*Status:\\s*ok\\b.*"));
        current(com.taktak.app.MainActivity.class);
    }
    private ActivityScenario<Activity> launchScreen(Intent intent) throws Exception {
        // ActivityScenario.close có thể gỡ cả task trên MIUI; khôi phục host QA mỗi lần mở.
        if (usbQa) launchQaHost();
        return ActivityScenario.launch(intent);
    }
    @Test public void replyFromViewerReachesPrivateChatInFourThemes() throws Exception {
        for (AppTheme theme : AppTheme.values()) {
            new ThemePreferences(context).save(theme, ColorMode.LIGHT);
            try (var screen = launchScreen(viewer(false))) {
                MomentViewerActivity activity = current(MomentViewerActivity.class);
                await(() -> shown(activity, R.id.viewerReplyPanel), "Authenticated photo and reply composer");
                int before = fixture.payloads.size();
                main(() -> {
                    ((EditText) activity.findViewById(R.id.viewerReplyInput)).setText("QA private reply " + theme.id());
                    activity.findViewById(R.id.viewerReplySend).performClick();
                });
                await(() -> fixture.payloads.size() == before + 1, "One explicit reply");
                ChatActivity chat = current(ChatActivity.class);
                await(() -> state(chat).loaded && !state(chat).busy, "Chat history loaded");
                var payload = fixture.payloads.get(before);
                assertEquals("MOMENT_REPLY", payload.messageType);
                assertEquals(ChatThemeFixture.MOMENT, payload.momentId);
                assertEquals("QA private reply " + theme.id(), payload.content);
                assertTrue(state(chat).messages.stream().anyMatch(m -> payload.content.equals(m.content)));
                capture("chat-reply-" + theme.id());
                closeScreens();
            }
        }
    }
    @Test public void invalidDraftTimeoutRetryAndRecreationKeepFrozenKey() throws Exception {
        try (var screen = launchScreen(viewer(false))) {
            MomentViewerActivity activity = current(MomentViewerActivity.class);
            await(() -> shown(activity, R.id.viewerReplyPanel), "Reply ready");
            main(() -> activity.findViewById(R.id.viewerReplySend).performClick());
            assertTrue(fixture.payloads.isEmpty());
            main(() -> ((EditText) activity.findViewById(R.id.viewerReplyInput)).setText("QA frozen reply"));
            fixture.failNextSend = true;
            main(() -> activity.findViewById(R.id.viewerReplySend).performClick());
            await(() -> reply(activity).state().getValue().pending && !reply(activity).state().getValue().busy
                    && reply(activity).state().getValue().error != 0, "Retry state after fixture 503");
            String key = fixture.payloads.get(0).clientMessageId;
            assertFalse(enabled(activity, R.id.viewerReplyInput));
            screen.recreate();
            MomentViewerActivity recreated = current(MomentViewerActivity.class);
            await(() -> shown(recreated, R.id.viewerReplyPanel), "Composer after recreation");
            assertEquals(1, fixture.payloads.size());
            capture("reply-retry-recreated");
            main(() -> recreated.findViewById(R.id.viewerReplySend).performClick());
            current(ChatActivity.class);
            assertEquals(2, fixture.payloads.size());
            assertEquals(key, fixture.payloads.get(1).clientMessageId);
            assertEquals("QA frozen reply", fixture.payloads.get(1).content);
            assertEquals(1, fixture.messages.size());
        }
    }
    @Test public void ownPhotoHiddenAndRevokedSessionDropsDraftWithoutSend() throws Exception {
        try (var screen = launchScreen(viewer(true))) {
            var activity = current(MomentViewerActivity.class);
            await(() -> enabled(activity, R.id.viewerSaveButton), "Own image loaded");
            assertFalse(shown(activity, R.id.viewerReplyPanel));
        }
        try (var screen = launchScreen(viewer(false))) {
            var activity = current(MomentViewerActivity.class);
            await(() -> shown(activity, R.id.viewerReplyPanel), "Other photo reply");
            main(() -> ((EditText) activity.findViewById(R.id.viewerReplyInput)).setText("QA unsent private draft"));
            fixture.client.sessions.beginLogin(); PushEventBus.emitSessionCleared();
            await(() -> reply(activity).state().getValue().revoked, "Session event revokes reply");
            main(() -> {
                assertEquals("", reply(activity).draft());
                assertEquals("", ((EditText) activity.findViewById(R.id.viewerReplyInput)).getText().toString());
                assertFalse(shown(activity, R.id.viewerReplyPanel));
            });
            assertTrue(fixture.payloads.isEmpty());
        }
    }
    @Test public void deletedPhotoHidesReferenceButKeepsReplyText() throws Exception {
        var item = new com.taktak.app.data.model.ChatModels.Message();
        item.id = java.util.UUID.randomUUID().toString(); item.conversationId = ChatThemeFixture.ROOM;
        item.senderId = ChatThemeFixture.SELF; item.recipientId = ChatThemeFixture.PEER;
        item.clientMessageId = java.util.UUID.randomUUID().toString(); item.messageType = "MOMENT_REPLY";
        item.momentId = ChatThemeFixture.MOMENT; item.content = "QA preserved reply";
        item.createdAt = "2026-10-20T00:00:00Z"; fixture.messages.add(item);
        fixture.denyImage = true;
        try (var screen = launchScreen(ChatActivity.intent(context, ChatThemeFixture.ROOM))) {
            var activity = current(ChatActivity.class);
            await(() -> state(activity).loaded && !state(activity).busy, "Reply in chat");
            await(() -> reference(activity).equals(context.getString(R.string.chat_moment_unavailable)), "Image failure does not erase reply");
            assertEquals("QA preserved reply", state(activity).messages.get(0).content);
            fixture.denyImage = false;
            main(() -> ((androidx.recyclerview.widget.RecyclerView) activity.findViewById(R.id.chatList)).getChildAt(0).performClick());
            await(() -> reference(activity).equals(context.getString(R.string.moment_reply_reference)), "Authenticated image retry restores reference label");
            fixture.denyImage = true; PushEventBus.emitMomentDeleted(ChatThemeFixture.MOMENT);
            await(() -> state(activity).messages.get(0).momentId == null, "Reference revoked");
            assertEquals("QA preserved reply", state(activity).messages.get(0).content);
            capture("reply-revoked-photo");
        }
    }
    private String reference(ChatActivity activity) {
        var value = new AtomicReference<String>("");
        main(() -> {
            TextView label = activity.findViewById(R.id.messageReference);
            if (label != null) value.set(label.getText().toString());
        });
        return value.get();
    }
    @Test public void physicalKeyboardLeavesSendReachable() throws Exception {
        try (var screen = launchScreen(viewer(false))) {
            var activity = current(MomentViewerActivity.class);
            await(() -> shown(activity, R.id.viewerReplyPanel), "Reply ready");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.viewerReplyInput))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click());
            main(() -> androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(), activity.findViewById(android.R.id.content))
                    .show(androidx.core.view.WindowInsetsCompat.Type.ime()));
            await(() -> {
                var value = new java.util.concurrent.atomic.AtomicBoolean();
                main(() -> { var insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.findViewById(android.R.id.content));
                    value.set(insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())); });
                return value.get();
            }, "Physical IME visible");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.viewerReplySend))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            capture("reply-ime"); androidx.test.espresso.Espresso.closeSoftKeyboard();
            assertTrue(fixture.payloads.isEmpty());
        }
    }
    @Test public void replyLayoutFitsThemesNightFontAndViewportMatrix() {
        main(() -> {
            for (AppTheme style : AppTheme.values()) for (int night : new int[]{
                    android.content.res.Configuration.UI_MODE_NIGHT_NO, android.content.res.Configuration.UI_MODE_NIGHT_YES})
                for (float font : new float[]{1f, 2f}) for (int[] size : new int[][]{{280, 560}, {360, 720}, {840, 420}}) {
                    var config = new android.content.res.Configuration(context.getResources().getConfiguration());
                    config.fontScale = font; config.uiMode = (config.uiMode & ~android.content.res.Configuration.UI_MODE_NIGHT_MASK) | night;
                    var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                            ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), ThemeManager.ScreenRole.VIEWER).themeResource());
                    View root = android.view.LayoutInflater.from(themed).inflate(R.layout.activity_moment_viewer, null, false);
                    root.findViewById(R.id.viewerReplyPanel).setVisibility(View.VISIBLE);
                    float density = themed.getResources().getDisplayMetrics().density;
                    int width = Math.round(size[0] * density), height = Math.round(size[1] * density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, width, height); horizontalBounds(root, width);
                }
        });
    }
    @Test public void processAndGenerationGuardRejectRestoredPrivateDraft() throws Exception {
        long currentGeneration = fixture.auth.sessionGeneration();
        main(() -> {
            var seed = new androidx.lifecycle.SavedStateHandle();
            var bootstrap = new MomentReplyViewModel((android.app.Application) context.getApplicationContext(), seed);
            String currentProcess = seed.get("momentReply.process");
            var bootstrapStore = new androidx.lifecycle.ViewModelStore(); bootstrapStore.put("qa-bootstrap", bootstrap); bootstrapStore.clear();
            for (int scenario = 0; scenario < 2; scenario++) {
                var saved = new androidx.lifecycle.SavedStateHandle();
                saved.set("momentReply.process", scenario == 0 ? "different-process" : currentProcess);
                saved.set("momentReply.generation", scenario == 0 ? currentGeneration : -900L);
                saved.set("momentReply.draft", "QA must not restore");
                saved.set("momentReply.key", java.util.UUID.randomUUID().toString());
                saved.set("momentReply.text", "QA must not send");
                saved.set("momentReply.sender", ChatThemeFixture.PEER); saved.set("momentReply.moment", ChatThemeFixture.MOMENT);
                var model = new MomentReplyViewModel((android.app.Application) context.getApplicationContext(), saved);
                var store = new androidx.lifecycle.ViewModelStore(); store.put("qa", model);
                try { assertEquals("", model.draft()); assertFalse(model.state().getValue().pending); }
                finally { store.clear(); }
            }
        });
        assertTrue(fixture.payloads.isEmpty());
    }
    private void horizontalBounds(View view, int width) {
        if (view.getVisibility() == View.GONE) return;
        assertTrue("Reply layout horizontal bounds " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof android.view.ViewGroup group)
            for (int index = 0; index < group.getChildCount(); index++) horizontalBounds(group.getChildAt(index), group.getWidth());
    }
    private MomentReplyViewModel reply(Activity activity) {
        var value = new AtomicReference<MomentReplyViewModel>();
        main(() -> value.set(new ViewModelProvider((androidx.lifecycle.ViewModelStoreOwner) activity).get(MomentReplyViewModel.class)));
        return value.get();
    }
    private ChatViewModel.State state(ChatActivity activity) {
        var value = new AtomicReference<ChatViewModel.State>();
        main(() -> value.set(new ViewModelProvider(activity).get(ChatViewModel.class).state().getValue())); return value.get();
    }
    private boolean shown(Activity activity, int id) {
        var value = new java.util.concurrent.atomic.AtomicBoolean(); main(() -> value.set(activity.findViewById(id).isShown())); return value.get();
    }
    private boolean enabled(Activity activity, int id) {
        var value = new java.util.concurrent.atomic.AtomicBoolean(); main(() -> value.set(activity.findViewById(id).isEnabled())); return value.get();
    }
    private <T extends Activity> T current(Class<T> type) throws Exception {
        var value = new AtomicReference<T>(); await(() -> {
            main(() -> { for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) if (type.isInstance(activity)) value.set(type.cast(activity)); });
            return value.get() != null;
        }, "Foreground " + type.getSimpleName()); return value.get();
    }
    private void await(BooleanSupplier check, String label) throws Exception {
        long end = android.os.SystemClock.uptimeMillis() + 20000;
        while (android.os.SystemClock.uptimeMillis() < end) { if (check.getAsBoolean()) return; android.os.SystemClock.sleep(80); }
        fail(label);
    }
    private void main(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) action.run();
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
    private void closeScreens() { closeScreens(false); }
    private void closeScreens(boolean closeHost) {
        main(() -> { var monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();
            for (var stage : androidx.test.runner.lifecycle.Stage.values()) for (Activity activity : new ArrayList<>(monitor.getActivitiesInStage(stage)))
                if (activity instanceof MomentViewerActivity || activity instanceof ChatActivity
                        || (closeHost && usbQa && activity instanceof com.taktak.app.MainActivity)) activity.finish(); });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(400);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(image);
        File folder = new File(context.getExternalFilesDir(null), usbQa ? "artifacts/moment-reply-usb" : "artifacts/moment-reply");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (var output = new FileOutputStream(new File(folder, name + ".png"))) { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output)); }
        finally { image.recycle(); }
    }
}
