package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.ui.auth.LoginActivity;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Kiểm tra native; chỉ tài khoản QA riêng, không thay phiên cá nhân hoặc cấu hình máy. */
@RunWith(AndroidJUnit4.class)
public class LoginM3Test {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa_m3_login", Context.MODE_PRIVATE);

    @Before public void preservePersonalSession() throws Exception {
        var store = new EncryptedSessionStore(context, BuildConfig.BASE_URL);
        if (store.load() != null) {
            String owner = fixture.getString("owner", "");
            assertFalse("Refusing to replace a personal session", owner.isEmpty());
            assertEquals("QA session ownership must match", owner, AuthGraph.repository(context).profile().id);
            AuthGraph.repository(context).logout();
        }
        assertTrue(fixture.edit().clear().commit());
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }

    @After public void finishOnlyQaActivities() throws Exception {
        if (!fixture.getString("owner", "").isEmpty()) {
            AuthGraph.repository(context).logout();
            assertTrue(fixture.edit().clear().commit());
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var activities = new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED));
            for (Activity activity : activities) { activity.finish(); }
        });
    }

    @Test public void responsiveLayoutAndContrastInBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                for (int[] size : new int[][]{{280, 560}, {360, 720}, {840, 420}}) {
                    var config = new Configuration(context.getResources().getConfiguration());
                    config.fontScale = 1.5f;
                    config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                    var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_TakTak_LoginReference);
                    float density = themed.getResources().getDisplayMetrics().density;
                    View root = LayoutInflater.from(themed).inflate(R.layout.activity_login, null, false);
                    ((TextInputLayout) root.findViewById(R.id.emailLayout)).setError(context.getString(R.string.auth_invalid_email));
                    TextView notice = root.findViewById(R.id.formNotice);
                    notice.setText(context.getString(R.string.auth_network_error)); notice.setVisibility(View.VISIBLE);
                    int width = Math.round(size[0] * density), height = Math.round(size[1] * density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, width, height);
                    bounds(root, width);
                    assertTrue(root.findViewById(R.id.loginContent).getWidth() <= Math.round(480 * density));
                    assertTrue(root.findViewById(R.id.submitButton).getHeight() >= Math.round(56 * density));
                    assertTrue(root.findViewById(R.id.switchButton).getHeight() >= Math.round(48 * density));
                    int[][] pairs = {{R.color.login_text, R.color.login_background},
                            {R.color.login_hint, R.color.login_background},
                            {R.color.login_accent, R.color.login_background}};
                    for (int[] pair : pairs) { assertTrue("Text contrast >= 4.5", ColorUtils.calculateContrast(
                            themed.getColor(pair[0]), themed.getColor(pair[1])) >= 4.5); }
                }
            }
        });
    }

    @Test public void lightDarkValidationLoadingAndPasswordState() throws Exception {
        launch();
        for (int mode : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
            switchMode(mode);
            capture(mode == AppCompatDelegate.MODE_NIGHT_NO ? "light.png" : "dark.png");
        }
        SessionRoutingTest.onResumed(activity -> {
            activity.findViewById(R.id.submitButton).performClick();
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.emailLayout)).getError());
            assertNotNull(((TextInputLayout) activity.findViewById(R.id.passwordLayout)).getError());
        });
        capture("validation.png");
        SessionRoutingTest.onResumed(activity -> {
            var login = (LoginActivity) activity;
            int before = login.findViewById(R.id.submitButton).getTop();
            login.setLoading(true);
            assertFalse(login.findViewById(R.id.emailInput).isEnabled());
            assertFalse(login.findViewById(R.id.passwordInput).isEnabled());
            assertFalse(login.findViewById(R.id.submitButton).isEnabled());
            assertFalse(login.findViewById(R.id.switchButton).isEnabled());
            assertEquals(View.VISIBLE, login.findViewById(R.id.loadingIndicator).getVisibility());
            assertEquals(before, login.findViewById(R.id.submitButton).getTop());
        });
        capture("loading.png");
        SessionRoutingTest.onResumed(activity -> {
            ((LoginActivity) activity).setLoading(false);
            ((TextInputLayout) activity.findViewById(R.id.emailLayout)).setError(null);
            ((TextInputLayout) activity.findViewById(R.id.passwordLayout)).setError(null);
            ((EditText) activity.findViewById(R.id.emailInput)).setText("layout@example.test");
            ((EditText) activity.findViewById(R.id.passwordInput)).setText(UUID.randomUUID().toString());
            assertNotNull("Password initially masked before tapping eye",
                    ((EditText) activity.findViewById(R.id.passwordInput)).getTransformationMethod());
        });
        onView(withId(R.id.passwordInput)).perform(scrollTo());
        onView(org.hamcrest.Matchers.allOf(withId(com.google.android.material.R.id.text_input_end_icon),
                isDescendantOfA(withId(R.id.passwordLayout)))).perform(click());
        SessionRoutingTest.onResumed(activity -> assertNull(((EditText) activity.findViewById(R.id.passwordInput)).getTransformationMethod()));
        final Activity[] previous = new Activity[1];
        SessionRoutingTest.onResumed(activity -> { previous[0] = activity; activity.recreate(); });
        await(() -> {
            AtomicBoolean changed = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    if (activity != previous[0] && activity instanceof LoginActivity) { changed.set(true); }
                }
            });
            return changed.get();
        }, "Login must recreate");
        SessionRoutingTest.onResumed(activity -> {
            assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
            assertEquals("layout@example.test", ((EditText) activity.findViewById(R.id.emailInput)).getText().toString());
        });
    }

    @Test public void keyboardKeepsPasswordVisibleAndRegisterStillWorks() throws Exception {
        launch();
        onView(withId(R.id.passwordInput)).perform(scrollTo(), click());
        SessionRoutingTest.onResumed(activity -> ViewCompat.getWindowInsetsController(activity.findViewById(android.R.id.content))
                .show(WindowInsetsCompat.Type.ime()));
        await(() -> {
            AtomicBoolean shown = new AtomicBoolean();
            SessionRoutingTest.onResumed(activity -> {
                var insets = ViewCompat.getRootWindowInsets(activity.findViewById(android.R.id.content));
                shown.set(insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()));
            });
            return shown.get();
        }, "IME must be visible");
        SystemClock.sleep(1200);
        SessionRoutingTest.onResumed(activity -> {
            View root = activity.findViewById(android.R.id.content), password = activity.findViewById(R.id.passwordInput);
            Rect visible = new Rect(); assertTrue(password.getGlobalVisibleRect(visible));
            int bottom = root.getHeight() - ViewCompat.getRootWindowInsets(root).getInsets(WindowInsetsCompat.Type.ime()).bottom;
            assertTrue("Password stays above IME", visible.bottom <= bottom);
            Rect local = new Rect(); assertTrue(password.getLocalVisibleRect(local));
            assertTrue("Entire password field stays visible", local.top <= 0 && local.bottom >= password.getHeight());
        });
        capture("keyboard.png");
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.switchButton)).perform(scrollTo(), click());
        SessionRoutingTest.awaitView(R.id.displayNameInput);
        SessionRoutingTest.onResumed(activity -> {
            assertTrue(activity instanceof com.taktak.app.ui.auth.RegisterActivity);
        });
        capture("register-unchanged.png");
        onView(withId(R.id.switchButton)).perform(scrollTo(), click());
        SessionRoutingTest.awaitView(R.id.emailInput);
    }

    @Test public void offlineErrorKeepsFormAndExplicitRetryLogsInThenLogsOut() throws Exception {
        Assume.assumeTrue("Requires host-managed backend connector", "true".equals(
                InstrumentationRegistry.getArguments().getString("taktakLoginOffline")));
        var repository = AuthGraph.repository(context);
        String username = "m3login_" + UUID.randomUUID().toString().replace("-", "");
        String email = username + "@example.test", password = UUID.randomUUID().toString();
        var owner = repository.register(email, username, "M3 Login QA", password);
        launch();
        SessionRoutingTest.onResumed(activity -> {
            ((EditText) activity.findViewById(R.id.emailInput)).setText(email);
            ((EditText) activity.findViewById(R.id.passwordInput)).setText(password);
        });
        android.util.Log.i("TaktakQa", "M3_LOGIN_READY_FOR_OFFLINE_" + InstrumentationRegistry.getArguments()
                .getString("taktakLoginRun", "default"));
        await(() -> !healthy(), "Host must disconnect QA backend");
        SessionRoutingTest.onResumed(activity -> activity.findViewById(R.id.submitButton).performClick());
        awaitMessage(R.string.auth_network_error);
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        capture("offline.png");
        await(this::healthy, "Host must reconnect QA backend");
        assertTrue(fixture.edit().putString("owner", owner.id).commit());
        SessionRoutingTest.onResumed(activity -> activity.findViewById(R.id.submitButton).performClick());
        SessionRoutingTest.awaitView(R.id.cameraTitle);
        assertEquals(owner.id, repository.profile().id);
        capture("retry-success.png");
        SessionRoutingTest.onResumed(activity -> activity.findViewById(R.id.logoutButton).performClick());
        awaitMessage(R.string.session_logged_out);
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        capture("logout.png");
    }

    private boolean healthy() {
        java.net.HttpURLConnection connection = null;
        try {
            connection = (java.net.HttpURLConnection) new java.net.URL(BuildConfig.BASE_URL + "actuator/health").openConnection();
            connection.setConnectTimeout(500); connection.setReadTimeout(500);
            return connection.getResponseCode() == 200;
        } catch (IOException unavailable) { return false; }
        finally { if (connection != null) { connection.disconnect(); } }
    }
    private void launch() throws Exception {
        var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             var reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder result = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) { result.append(line); }
            assertTrue("Launcher starts normally", result.toString().contains("Status: ok"));
        }
        SessionRoutingTest.awaitView(R.id.emailInput);
    }
    private void switchMode(int mode) {
        SessionRoutingTest.onResumed(activity -> ((androidx.appcompat.app.AppCompatActivity) activity).getDelegate().setLocalNightMode(mode));
        await(() -> {
            AtomicBoolean matches = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    int night = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    matches.set(activity instanceof LoginActivity && night == (mode == AppCompatDelegate.MODE_NIGHT_NO
                            ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES));
                }
            }); return matches.get();
        }, "Requested theme must be active");
    }
    private void awaitMessage(int resource) {
        await(() -> {
            AtomicBoolean matches = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    TextView notice = activity.findViewById(R.id.formNotice);
                    if (notice != null && notice.isShown() && notice.getText().toString().equals(activity.getString(resource))) {
                        matches.set(true);
                    }
                }
            }); return matches.get();
        }, "Expected authentication message");
    }
    private void await(java.util.function.BooleanSupplier condition, String message) {
        long deadline = SystemClock.elapsedRealtime() + 45000;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition.getAsBoolean()) { return; }
            SystemClock.sleep(100);
        }
        fail(message);
    }
    private void bounds(View view, int parentWidth) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if (view instanceof TextView text && !(text instanceof EditText) && text.getLayout() != null) {
            assertTrue("Text fits vertically: " + text.getClass().getSimpleName(), text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) { bounds(group.getChildAt(index), group.getWidth()); }
        }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(1000);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/m3e-login");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
