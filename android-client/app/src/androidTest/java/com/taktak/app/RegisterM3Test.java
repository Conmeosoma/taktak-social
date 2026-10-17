package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.data.model.AuthModels.*;
import com.taktak.app.data.remote.AuthApi;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.ui.auth.RegisterActivity;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Register không tự tạo phiên: mọi ca từ chối chạy nếu đang có phiên cá nhân. */
@RunWith(AndroidJUnit4.class)
public class RegisterM3Test {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before public void preserveSession() throws Exception {
        assertNull("Refusing to replace an existing session", new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }
    @After public void finishQaActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var activities = new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED));
            for (Activity activity : activities) { activity.finish(); }
        });
    }

    @Test public void longFormFitsNarrowLargeTextAndWideWindows() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                for (int[] size : new int[][]{{280, 560}, {360, 720}, {840, 420}}) {
                    var config = new Configuration(context.getResources().getConfiguration());
                    config.fontScale = 1.5f;
                    config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                    var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_TakTak_M3_Register);
                    float density = themed.getResources().getDisplayMetrics().density;
                    View root = LayoutInflater.from(themed).inflate(R.layout.activity_register, null, false);
                    for (int field : new int[]{R.id.displayNameLayout, R.id.usernameLayout, R.id.emailLayout,
                            R.id.passwordLayout, R.id.confirmPasswordLayout}) {
                        ((TextInputLayout) root.findViewById(field)).setError(context.getString(R.string.auth_invalid_password));
                    }
                    TextView notice = root.findViewById(R.id.formNotice);
                    notice.setText(R.string.auth_network_error); notice.setVisibility(View.VISIBLE);
                    int width = Math.round(size[0] * density), height = Math.round(size[1] * density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, width, height);
                    bounds(root, width);
                    assertTrue(root.findViewById(R.id.registerContent).getWidth() <= Math.round(480 * density));
                    assertTrue(root.findViewById(R.id.submitButton).getHeight() >= Math.round(56 * density));
                    assertTrue(root.findViewById(R.id.switchButton).getHeight() >= Math.round(48 * density));
                    var scroll = (androidx.core.widget.NestedScrollView) root.findViewById(R.id.authScroll);
                    assertTrue("Long form scrolls instead of clipping", scroll.getChildAt(0).getHeight() > scroll.getHeight());
                    scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
                    assertTrue("Last action stays inside scroll content", root.findViewById(R.id.switchButton).getBottom()
                            <= root.findViewById(R.id.registerContent).getHeight());
                }
            }
        });
    }

    @Test public void lightDarkValidationLoadingToggleAndRecreate() throws Exception {
        launchRegister();
        for (int mode : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
            onRegister(activity -> activity.getDelegate().setLocalNightMode(mode));
            await(() -> activityMatches(activity -> (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == (mode == AppCompatDelegate.MODE_NIGHT_NO ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES)), "Theme must apply");
            onView(withId(R.id.authBrand)).perform(scrollTo());
            capture(mode == AppCompatDelegate.MODE_NIGHT_NO ? "light-top.png" : "dark-top.png");
            onView(withId(R.id.switchButton)).perform(scrollTo());
            capture(mode == AppCompatDelegate.MODE_NIGHT_NO ? "light-bottom.png" : "dark-bottom.png");
        }
        onView(withId(R.id.submitButton)).perform(scrollTo(), click());
        onRegister(activity -> {
            for (int field : new int[]{R.id.displayNameLayout, R.id.usernameLayout, R.id.emailLayout, R.id.passwordLayout}) {
                assertNotNull(((TextInputLayout) activity.findViewById(field)).getError());
            }
            assertTrue(activity.findViewById(R.id.displayNameInput).hasFocus());
            input(activity, R.id.displayNameInput, "Người dùng QA 📷");
            input(activity, R.id.usernameInput, "not.valid");
            input(activity, R.id.emailInput, "not-an-email");
            String password = UUID.randomUUID().toString();
            input(activity, R.id.passwordInput, password); input(activity, R.id.confirmPasswordInput, password + " ");
            activity.findViewById(R.id.submitButton).performClick();
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.usernameLayout)).getError());
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.emailLayout)).getError());
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.confirmPasswordLayout)).getError());
            assertTrue(ViewCompat.isAccessibilityHeading(activity.findViewById(R.id.registerIdentity)));
            assertTrue(ViewCompat.isAccessibilityHeading(activity.findViewById(R.id.registerSecurity)));
            activity.setLoading(true);
            for (int id : new int[]{R.id.displayNameInput, R.id.usernameInput, R.id.emailInput,
                    R.id.passwordInput, R.id.confirmPasswordInput, R.id.submitButton, R.id.switchButton}) {
                assertFalse(activity.findViewById(id).isEnabled());
            }
            assertEquals(View.VISIBLE, activity.findViewById(R.id.loadingIndicator).getVisibility());
        });
        onView(withId(R.id.submitButton)).perform(scrollTo());
        capture("loading.png");
        onRegister(activity -> {
            activity.setLoading(false);
            ((TextInputLayout) activity.findViewById(R.id.confirmPasswordLayout)).setError(null);
        });
        onView(withId(R.id.confirmPasswordInput)).perform(scrollTo());
        onView(org.hamcrest.Matchers.allOf(withId(com.google.android.material.R.id.text_input_end_icon),
                isDescendantOfA(withId(R.id.confirmPasswordLayout)))).perform(click());
        onRegister(activity -> assertNull(((EditText) activity.findViewById(R.id.confirmPasswordInput)).getTransformationMethod()));
        final Activity[] previous = new Activity[1];
        onRegister(activity -> { previous[0] = activity; activity.recreate(); });
        await(() -> activityMatches(activity -> activity != previous[0]), "Register must recreate");
        onRegister(activity -> {
            assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
            assertEquals("", ((EditText) activity.findViewById(R.id.confirmPasswordInput)).getText().toString());
            assertEquals("Người dùng QA 📷", ((EditText) activity.findViewById(R.id.displayNameInput)).getText().toString());
            assertTrue(activity.findViewById(R.id.submitButton).isEnabled());
        });
    }

    @Test public void lastFieldAboveKeyboardAndLoginNavigation() throws Exception {
        launchRegister();
        onView(withId(R.id.confirmPasswordInput)).perform(scrollTo(), click());
        onRegister(activity -> ViewCompat.getWindowInsetsController(activity.findViewById(android.R.id.content)).show(WindowInsetsCompat.Type.ime()));
        await(() -> activityMatches(activity -> {
            var insets = ViewCompat.getRootWindowInsets(activity.findViewById(android.R.id.content));
            return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        }), "IME must actually open");
        SystemClock.sleep(1200);
        onRegister(activity -> {
            View root = activity.findViewById(android.R.id.content), field = activity.findViewById(R.id.confirmPasswordInput);
            Rect global = new Rect(), local = new Rect();
            assertTrue(field.getGlobalVisibleRect(global)); assertTrue(field.getLocalVisibleRect(local));
            assertTrue("Entire confirmation field visible", local.top <= 0 && local.bottom >= field.getHeight());
            int bottom = root.getHeight() - ViewCompat.getRootWindowInsets(root).getInsets(WindowInsetsCompat.Type.ime()).bottom;
            assertTrue("Confirmation stays above keyboard", global.bottom <= bottom);
        });
        capture("keyboard.png");
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.switchButton)).perform(scrollTo(), click());
        await(() -> activityOf(com.taktak.app.ui.auth.LoginActivity.class), "Returns to Login");
        capture("return-login.png");
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        onView(withId(R.id.switchButton)).perform(scrollTo(), click());
        await(() -> activityOf(RegisterActivity.class), "Can open Register again");
    }

    @Test public void offlineRetryCreatesRealAccountWithoutAutoLoginAndHandlesConflict() throws Exception {
        Assume.assumeTrue("Requires host-managed QA connector", "true".equals(
                InstrumentationRegistry.getArguments().getString("taktakRegisterOffline")));
        String username = "m3reg_" + UUID.randomUUID().toString().replace("-", "");
        String email = username + "@example.test", password = UUID.randomUUID().toString();
        launchRegister();
        onRegister(activity -> {
            input(activity, R.id.displayNameInput, "  Bạn mới QA 📷  ");
            input(activity, R.id.usernameInput, "  " + username.toUpperCase(java.util.Locale.ROOT) + "  ");
            input(activity, R.id.emailInput, "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ");
            input(activity, R.id.passwordInput, password); input(activity, R.id.confirmPasswordInput, password);
        });
        onView(withId(R.id.submitButton)).perform(scrollTo());
        android.util.Log.i("TaktakQa", "M3_REGISTER_READY_OFFLINE_" + InstrumentationRegistry.getArguments().getString("taktakQaRun", "default"));
        await(() -> !healthy(), "Host disconnects QA backend");
        onView(withId(R.id.submitButton)).perform(click());
        awaitNotice(R.string.auth_network_error);
        onView(withId(R.id.formNotice)).perform(scrollTo()); capture("offline.png");
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        await(this::healthy, "Host reconnects QA backend");
        onView(withId(R.id.submitButton)).perform(scrollTo(), click());
        awaitNotice(R.string.auth_register_success);
        onRegister(activity -> {
            assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
            assertEquals("", ((EditText) activity.findViewById(R.id.confirmPasswordInput)).getText().toString());
        });
        assertNull("Register must not automatically save tokens", new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        onView(withId(R.id.formNotice)).perform(scrollTo()); capture("success.png");
        AuthApi raw = new retrofit2.Retrofit.Builder().baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(AuthApi.class);
        var login = raw.login(new LoginRequest(email, password)).execute();
        assertEquals("Created account can authenticate against real server", 200, login.code());
        assertNotNull(login.body());
        var logout = raw.logout(new RefreshRequest(login.body().refreshToken)).execute();
        assertEquals(204, logout.code());
        onRegister(activity -> { input(activity, R.id.passwordInput, password); input(activity, R.id.confirmPasswordInput, password); });
        onView(withId(R.id.submitButton)).perform(scrollTo(), click());
        awaitNotice(R.string.auth_account_conflict);
        onView(withId(R.id.formNotice)).perform(scrollTo()); capture("conflict.png");
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        onRegister(activity -> { input(activity, R.id.passwordInput, ""); input(activity, R.id.confirmPasswordInput, ""); });
    }

    private void launchRegister() throws Exception {
        var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             var reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder result = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) { result.append(line); }
            assertTrue("Launcher must start", result.toString().contains("Status: ok"));
        }
        await(() -> activityOf(com.taktak.app.ui.auth.LoginActivity.class), "Launcher reaches Login");
        onView(withId(R.id.switchButton)).perform(scrollTo(), click());
        await(() -> activityOf(RegisterActivity.class), "Register opens");
    }
    private void input(Activity activity, int id, String value) { ((EditText) activity.findViewById(id)).setText(value); }
    private void onRegister(java.util.function.Consumer<RegisterActivity> action) {
        SessionRoutingTest.onResumed(activity -> { assertTrue(activity instanceof RegisterActivity); action.accept((RegisterActivity) activity); });
    }
    private boolean activityOf(Class<? extends Activity> type) {
        AtomicBoolean found = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) { if (type.isInstance(activity)) { found.set(true); } }
        }); return found.get();
    }
    private boolean activityMatches(java.util.function.Predicate<RegisterActivity> predicate) {
        AtomicBoolean found = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                if (activity instanceof RegisterActivity register && predicate.test(register)) { found.set(true); }
            }
        }); return found.get();
    }
    private void awaitNotice(int resource) {
        await(() -> activityMatches(activity -> {
            TextView notice = activity.findViewById(R.id.formNotice);
            return notice != null && notice.getVisibility() == View.VISIBLE && notice.getText().toString().equals(activity.getString(resource));
        }), "Expected Register UI result");
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
    private void bounds(View view, int parentWidth) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if (view instanceof TextView text && !(text instanceof EditText) && text.getLayout() != null) {
            assertTrue("Text fits vertically: " + view.getClass().getSimpleName(), text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) { for (int i = 0; i < group.getChildCount(); i++) { bounds(group.getChildAt(i), group.getWidth()); } }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(1000);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/m3e-register");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)); }
        finally { bitmap.recycle(); }
    }
}
