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
import com.taktak.app.ui.auth.LoginActivity;
import com.taktak.app.ui.history.MomentViewerActivity;
import com.taktak.app.viewmodel.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

/**
 * UI-THEME-08: Profile screen, Avatar upload & Logout across 4 themes (Original, Gradient, Glass, Soft).
 * Fixture Retrofit trong RAM, không network, không thay auth store thật của người dùng.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfileThemeUiTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(360);
    private Context context;
    private MediaThemeFixture fixture;
    private Map<String, ?> original;
    private File sessionFile;
    private byte[] sessionBytes;
    private boolean foreground;

    @Before public void setup() throws Exception {
        String optIn = InstrumentationRegistry.getArguments().getString("taktak.allowMediaQa");
        if (optIn != null) {
            assertEquals("Explicit USB QA opt-in", "true", optIn);
        }
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(context.getPackageName(), android.Manifest.permission.CAMERA);
        original = new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).getAll());
        sessionFile = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        sessionBytes = sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null;
        fixture = new MediaThemeFixture(context);
        fixture.mockProfile = true;
    }

    @After public void cleanup() throws Exception {
        closeScreens();
        if (fixture != null) {
            fixture.close();
        }
        if (original != null) {
            var editor = context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).edit();
            for (String key : new String[]{ThemePreferences.STYLE, ThemePreferences.MODE, ThemePreferences.SCHEMA, ThemePreferences.REVISION}) {
                Object value = original.get(key);
                if (!original.containsKey(key)) editor.remove(key);
                else if (value instanceof String s) editor.putString(key, s);
                else if (value instanceof Integer i) editor.putInt(key, i);
                else if (value instanceof Long l) editor.putLong(key, l);
                else throw new AssertionError("Unsupported existing appearance type");
            }
            assertTrue(editor.commit());
            if (sessionBytes == null) {
                if (sessionFile.exists()) sessionFile.delete();
            } else {
                assertArrayEquals("Personal session bytes unchanged", sessionBytes, sessionFile.exists()
                        ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null);
            }
        }
    }

    @Test public void test01_allStylesModesLayoutsForProfile() {
        main(() -> {
            for (AppTheme style : AppTheme.values()) {
                for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                    for (float font : new float[]{1f, 1.5f, 2f}) {
                        for (int[] size : new int[][]{{280, 560}, {360, 720}, {840, 420}}) {
                            var config = new Configuration(context.getResources().getConfiguration());
                            config.fontScale = font;
                            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                            var themed = new androidx.appcompat.view.ContextThemeWrapper(
                                    context.createConfigurationContext(config),
                                    ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), ThemeManager.ScreenRole.MAIN).themeResource());
                            float density = themed.getResources().getDisplayMetrics().density;
                            int w = Math.round(size[0] * density);
                            int h = Math.round(size[1] * density);

                            View root = LayoutInflater.from(themed).inflate(R.layout.fragment_profile, null, false);
                            TextView title = root.findViewById(R.id.profileTitle);
                            TextView username = root.findViewById(R.id.profileUsername);
                            TextView identity = root.findViewById(R.id.profileIdentity);
                            EditText nameInput = root.findViewById(R.id.profileNameInput);
                            TextView message = root.findViewById(R.id.profileMessage);
                            Button save = root.findViewById(R.id.profileSave);
                            Button retry = root.findViewById(R.id.profileRetry);
                            Button logout = root.findViewById(R.id.profileLogout);
                            Button themeBtn = root.findViewById(R.id.profileThemeButton);
                            View avatar = root.findViewById(R.id.profileAvatar);
                            View avatarEdit = root.findViewById(R.id.profileAvatarEdit);
                            View changeAvatar = root.findViewById(R.id.profileChangeAvatar);

                            assertNotNull("Profile title exists", title);
                            assertNotNull("Profile username exists", username);
                            assertNotNull("Profile identity exists", identity);
                            assertNotNull("Profile nameInput exists", nameInput);
                            assertNotNull("Profile save exists", save);
                            assertNotNull("Profile retry exists", retry);
                            assertNotNull("Profile logout exists", logout);
                            assertNotNull("Profile themeButton exists", themeBtn);
                            assertNotNull("Profile avatar exists", avatar);
                            assertNotNull("Profile avatarEdit exists", avatarEdit);
                            assertNotNull("Profile changeAvatar exists", changeAvatar);

                            username.setVisibility(View.VISIBLE);
                            username.setText("@qa_profile");
                            identity.setVisibility(View.VISIBLE);
                            identity.setText("qa_profile@example.com");
                            nameInput.setText("QA Profile User");
                            message.setVisibility(View.VISIBLE);
                            message.setText(R.string.profile_saved);

                            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                            root.layout(0, 0, w, h);

                            bounds(root, w);

                            int bg = color(themed, com.google.android.material.R.attr.colorSurface);
                            int ink = color(themed, com.google.android.material.R.attr.colorOnSurface);
                            assertTrue("Profile surface contrast WCAG >= 4.5 (" + style.id() + ")",
                                    androidx.core.graphics.ColorUtils.calculateContrast(ink, bg) >= 4.5);
                        }
                    }
                }
            }
        });
    }

    @Test public void test02_profileDetailsAndDisplayNameEditSurvivesThemeChange() throws Exception {
        for (AppTheme style : AppTheme.values()) {
            select(style, ColorMode.LIGHT);
            MainActivity activity = launch(MainActivity.class);
            authorize(activity);
            main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_profile));
            await(() -> visible(activity, R.id.profileUsername), "Profile loaded for " + style.id());

            var profileModel = new AtomicReference<ProfileViewModel>();
            main(() -> profileModel.set(new ViewModelProvider(activity).get(ProfileViewModel.class)));
            await(() -> {
                var s = profileModel.get().state().getValue();
                return s != null && s.profile != null && !s.busy;
            }, "Profile state populated for " + style.id());

            main(() -> {
                TextView username = activity.findViewById(R.id.profileUsername);
                assertTrue(username.getText().toString().contains("qa_profile"));
                TextView identity = activity.findViewById(R.id.profileIdentity);
                assertEquals("qa_profile@example.com", identity.getText().toString());
                EditText nameInput = activity.findViewById(R.id.profileNameInput);
                nameInput.setText("QA Name " + style.id());
            });

            main(() -> activity.findViewById(R.id.profileSave).performClick());
            await(() -> {
                var s = profileModel.get().state().getValue();
                return s != null && s.message == R.string.profile_saved && !s.busy;
            }, "Profile saved for " + style.id());

            capture("profile-saved-" + style.id());

            main(() -> {
                TextView message = activity.findViewById(R.id.profileMessage);
                assertEquals(View.VISIBLE, message.getVisibility());
                assertEquals(activity.getString(R.string.profile_saved), message.getText().toString());
                assertEquals("QA Name " + style.id(), fixture.profileDisplayName);
            });

            closeScreens();
        }

        // Test theme pending / recreation preserves profile details
        select(AppTheme.GRADIENT, ColorMode.LIGHT);
        MainActivity activity = launch(MainActivity.class);
        authorize(activity);
        main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_profile));
        await(() -> visible(activity, R.id.profileUsername), "Profile visible before recreation");

        select(AppTheme.GLASS, ColorMode.LIGHT);
        main(() -> activity.recreate());
        MainActivity recreated = awaitActivity(MainActivity.class, activity);
        authorize(recreated);
        main(() -> ((BottomNavigationView) recreated.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_profile));
        await(() -> visible(recreated, R.id.profileUsername), "Profile visible after recreation");

        main(() -> {
            TextView identity = recreated.findViewById(R.id.profileIdentity);
            assertEquals("qa_profile@example.com", identity.getText().toString());
            EditText nameInput = recreated.findViewById(R.id.profileNameInput);
            assertNotNull(nameInput.getText());
            assertTrue(nameInput.getText().length() > 0);
        });

        closeScreens();
    }

    @Test public void test03_profileAvatarAndLogoutInteractionThemedCorrectly() throws Exception {
        select(AppTheme.SOFT, ColorMode.LIGHT);
        MainActivity activity = launch(MainActivity.class);
        authorize(activity);
        main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_profile));
        await(() -> visible(activity, R.id.profileUsername), "Profile visible for avatar test");

        var profileModel = new AtomicReference<ProfileViewModel>();
        main(() -> profileModel.set(new ViewModelProvider(activity).get(ProfileViewModel.class)));
        await(() -> {
            var s = profileModel.get().state().getValue();
            return s != null && s.profile != null && !s.busy;
        }, "Profile state populated");

        // Verify avatar buttons are enabled
        assertTrue("Change avatar button enabled", enabled(activity, R.id.profileChangeAvatar));
        assertTrue("Avatar edit button enabled", enabled(activity, R.id.profileAvatarEdit));

        // Simulate avatar upload via model
        File avatarFile = new File(context.getCacheDir(), "qa_avatar.jpg");
        Bitmap bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.MAGENTA);
        try (var fos = new FileOutputStream(avatarFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } finally {
            bitmap.recycle();
        }
        android.net.Uri avatarUri = android.net.Uri.fromFile(avatarFile);
        try {
            main(() -> profileModel.get().upload(avatarUri));
            await(() -> {
                var s = profileModel.get().state().getValue();
                return s != null && s.message == R.string.profile_avatar_saved && !s.busy;
            }, "Avatar upload complete");
            assertTrue("Avatar uploads dispatched", fixture.avatarUploads.get() > 0);

            capture("profile-avatar-uploaded");
        } finally {
            avatarFile.delete();
        }

        // Verify Theme Settings button opens theme settings
        main(() -> activity.findViewById(R.id.profileThemeButton).performClick());
        ThemeSettingsActivity themeActivity = awaitActivity(ThemeSettingsActivity.class, activity);
        assertNotNull(themeActivity);
        main(themeActivity::finish);
        await(() -> foreground(activity), "MainActivity regains focus");

        // Test Logout interaction on same screen
        await(() -> visible(activity, R.id.profileLogout), "Profile logout visible");
        main(() -> activity.findViewById(R.id.profileLogout).performClick());
        await(() -> activity.isFinishing(), "MainActivity finishes after logout click");

        assertFalse("Session cleared on logout", fixture.auth.hasSession());
    }

    private int color(Context themed, int attr) {
        return MaterialColors.getColor(themed, attr, Color.TRANSPARENT);
    }

    private void bounds(View view, int width) {
        if (view.getVisibility() == View.GONE) return;
        String idName;
        try { idName = view.getId() != View.NO_ID ? view.getResources().getResourceEntryName(view.getId()) : "none"; }
        catch (Exception e) { idName = "none"; }
        assertTrue("Horizontal bounds: " + view.getClass().getSimpleName() + " [" + idName + "] left=" + view.getLeft() + " right=" + view.getRight() + " max=" + width,
                view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof TextView text && !(text instanceof EditText) && text.getText() != null && text.getText().length() > 0 && text.getLayout() != null)
            assertTrue("Text fits height: " + view.getClass().getSimpleName() + " [" + idName + "]",
                    text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) bounds(group.getChildAt(i), group.getWidth());
        }
    }

    private void select(AppTheme style, ColorMode mode) throws Exception { new ThemePreferences(context).save(style, mode); }

    private void warm() throws Exception {
        if (foreground) return;
        try (var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
             var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
        foreground = true;
    }

    private <T extends Activity> T launch(Class<T> type) throws Exception { return launch(type, new Intent(context, type)); }

    private <T extends Activity> T launch(Class<T> type, Intent intent) throws Exception {
        warm();
        if(type==MainActivity.class) {
            T result=awaitActivity(type,null);
            await(() -> foreground(result),"Main shell has focus after USB shell launch");
            return result;
        }
        var result = type.cast(InstrumentationRegistry.getInstrumentation().startActivitySync(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        await(() -> foreground(result), "Screen has focus");
        return result;
    }

    private <T extends Activity> T awaitActivity(Class<T> type, Activity previous) throws Exception {
        var result = new AtomicReference<T>();
        await(() -> {
            main(() -> {
                for (var activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
                    if (type.isInstance(activity) && activity != previous) result.set(type.cast(activity));
            });
            return result.get() != null;
        }, "Activity resumes: " + type.getSimpleName());
        return result.get();
    }

    @SuppressWarnings("unchecked") private void authorize(Activity activity) throws Exception {
        await(() -> {
            var state = new AtomicReference<SessionViewModel.State>();
            main(() -> state.set(new ViewModelProvider((ViewModelStoreOwner) activity).get(SessionViewModel.class).state().getValue()));
            return state.get() != null && (state.get().destination == SessionViewModel.Destination.ERROR || state.get().destination == SessionViewModel.Destination.CAMERA);
        }, "Mock profile blocked before FCM/widget");
        main(() -> ((MutableLiveData<SessionViewModel.State>) new ViewModelProvider((ViewModelStoreOwner) activity)
                .get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA, 0)));
    }

    private boolean enabled(Activity activity, int id) {
        var result = new AtomicBoolean();
        main(() -> { View view = activity.findViewById(id); result.set(view != null && view.isEnabled()); });
        return result.get();
    }

    private boolean visible(Activity activity, int id) {
        var result = new AtomicBoolean();
        main(() -> result.set(activity.findViewById(id) != null && activity.findViewById(id).isShown()));
        return result.get();
    }

    private boolean foreground(Activity activity) {
        var result = new AtomicBoolean();
        main(() -> {
            boolean focus = activity.getWindow() != null && activity.getWindow().getDecorView().hasWindowFocus();
            var stage = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity);
            result.set(!activity.isFinishing() && (focus || stage == androidx.test.runner.lifecycle.Stage.RESUMED));
        });
        return result.get();
    }

    private void await(BooleanSupplier condition, String message) throws Exception {
        long until = android.os.SystemClock.uptimeMillis() + 18000;
        while (android.os.SystemClock.uptimeMillis() < until) {
            if (condition.getAsBoolean()) return;
            android.os.SystemClock.sleep(80);
        }
        fail(message);
    }

    private void main(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) action.run();
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private void closeScreens() {
        main(() -> {
            var monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();
            for (var stage : androidx.test.runner.lifecycle.Stage.values())
                for (Activity activity : new ArrayList<>(monitor.getActivitiesInStage(stage)))
                    if (activity instanceof MainActivity || activity instanceof MomentViewerActivity || activity instanceof ThemeSettingsActivity || activity instanceof LoginActivity) {
                        activity.finish();
                    }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        foreground = false;
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(500);
        var image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        var folder = new File(context.getExternalFilesDir(null), "artifacts/theme08");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (var out = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally {
            image.recycle();
        }
    }
}
