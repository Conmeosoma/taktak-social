package com.taktak.app.ui.theme;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.R;
import com.taktak.app.ui.auth.LoginActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/** Fixture chỉ trên AVD QA read-only; không đăng nhập hoặc gửi request thật. */
@RunWith(AndroidJUnit4.class)
public class LoginThemeUiTest {
    private Context context;
    @Before public void setup() {
        assertTrue(android.os.Build.HARDWARE.contains("ranchu") || android.os.Build.HARDWARE.contains("goldfish"));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteSharedPreferences(ThemePreferences.FILE_NAME);
    }
    @After public void cleanup() { if (context != null) context.deleteSharedPreferences(ThemePreferences.FILE_NAME); }
    private ActivityScenario<LoginActivity> open() {
        return ActivityScenario.launch(new Intent(context, LoginActivity.class));
    }
    private int color(Context themed, int attr) { return MaterialColors.getColor(themed, attr, "QA"); }
    @Test public void fourStylesBothModesRenderValidateLoadAndToggleEye() throws Exception {
        for (AppTheme style : AppTheme.values()) for (ColorMode mode : new ColorMode[]{ColorMode.LIGHT, ColorMode.DARK}) {
            new ThemePreferences(context).save(style, mode);
            try (var screen = open()) {
                capture(style.id() + "-" + mode.id());
                screen.onActivity(activity -> {
                    assertEquals(mode == ColorMode.DARK ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO,
                            activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
                    assertEquals(style == AppTheme.ORIGINAL ? 0 : style.ordinal(),
                            attribute(activity, R.attr.ttLoginKind));
                    activity.findViewById(R.id.submitButton).performClick();
                    assertNotNull(((TextInputLayout)activity.findViewById(R.id.emailLayout)).getError());
                    assertNotNull(((TextInputLayout)activity.findViewById(R.id.passwordLayout)).getError());
                    activity.setLoading(true);
                    for (int id : new int[]{R.id.submitButton, R.id.switchButton, R.id.loginThemeButton,
                            R.id.forgotPasswordLabel, R.id.verifyEmailButton}) assertFalse(activity.findViewById(id).isEnabled());
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.loadingIndicator).getVisibility());
                    activity.setLoading(false);
                    var password = (EditText)activity.findViewById(R.id.passwordInput);
                    assertFalse("Never save password in view state", password.isSaveEnabled());
                    password.setText("qa-draft-only");
                    var layout = (TextInputLayout)activity.findViewById(R.id.passwordLayout);
                    layout.setError(null);
                    View eye = layout.findViewById(com.google.android.material.R.id.text_input_end_icon);
                    eye.performClick(); assertNull(password.getTransformationMethod());
                    eye.performClick(); assertNotNull(password.getTransformationMethod());
                    password.setText("");
                    ((TextInputLayout)activity.findViewById(R.id.emailLayout)).setError(null);
                    activity.findViewById(R.id.emailInput).clearFocus();
                });
                androidx.test.espresso.Espresso.closeSoftKeyboard();
            }
        }
    }
    @Test public void newThemesSupportPhysicalImeAndRecoveryRegisterNavigation() throws Exception {
        for (AppTheme style : new AppTheme[]{AppTheme.GRADIENT, AppTheme.GLASS, AppTheme.SOFT}) {
            new ThemePreferences(context).save(style, ColorMode.SYSTEM);
            try (var screen = open()) {
                onView(withId(R.id.passwordInput)).perform(scrollTo(), click());
                screen.onActivity(activity -> androidx.core.view.WindowCompat.getInsetsController(
                        activity.getWindow(), activity.findViewById(android.R.id.content))
                        .show(androidx.core.view.WindowInsetsCompat.Type.ime()));
                long deadline = android.os.SystemClock.uptimeMillis() + 10000;
                var shown = new java.util.concurrent.atomic.AtomicBoolean();
                while (!shown.get() && android.os.SystemClock.uptimeMillis() < deadline) {
                    screen.onActivity(activity -> {
                        var insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.findViewById(android.R.id.content));
                        shown.set(insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
                    });
                    android.os.SystemClock.sleep(100);
                }
                assertTrue("Physical IME shown in " + style.id(), shown.get());
                onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()));
                capture(style.id() + "-ime");
                androidx.test.espresso.Espresso.closeSoftKeyboard();
                onView(withId(R.id.forgotPasswordLabel)).perform(scrollTo(), click());
                assertForeground(com.taktak.app.ui.auth.ForgotPasswordActivity.class);
                androidx.test.espresso.Espresso.pressBack();
                onView(withId(R.id.verifyEmailButton)).perform(scrollTo(), click());
                assertForeground(com.taktak.app.ui.auth.EmailVerificationActivity.class);
                androidx.test.espresso.Espresso.pressBack();
                onView(withId(R.id.switchButton)).perform(scrollTo(), click());
                assertForeground(com.taktak.app.ui.auth.RegisterActivity.class);
                androidx.test.espresso.Espresso.pressBack();
            }
        }
    }
    private void assertForeground(Class<?> expected) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var activities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
            assertTrue("Expected foreground " + expected.getSimpleName(), activities.stream().anyMatch(expected::isInstance));
        });
    }
    @Test public void layoutAndContrastMatrix() {
        var runner = InstrumentationRegistry.getInstrumentation();
        runner.runOnMainSync(() -> {
            for (AppTheme style : AppTheme.values()) for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES})
                for (float scale : new float[]{1f, 1.5f, 2f}) for (int[] size : new int[][]{{280,560},{360,640},{840,420}}) {
                    var config = new Configuration(context.getResources().getConfiguration());
                    config.fontScale = scale; config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                    var resolved = ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), ThemeManager.ScreenRole.LOGIN);
                    var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), resolved.themeResource());
                    int surface = color(themed, com.google.android.material.R.attr.colorSurface);
                    int field = androidx.core.graphics.ColorUtils.compositeColors(color(themed, R.attr.ttLoginField), surface);
                    for (int attr : new int[]{com.google.android.material.R.attr.colorOnSurface, com.google.android.material.R.attr.colorOnSurfaceVariant}) {
                        assertTrue("Input text contrast >=4.5", androidx.core.graphics.ColorUtils.calculateContrast(color(themed, attr), field) >= 4.5);
                    }
                    if (style != AppTheme.ORIGINAL) for (int attr : new int[]{R.attr.ttLoginStart, R.attr.ttLoginEnd}) {
                        assertTrue("CTA contrast >=4.5: " + style.id() + "/" + night, androidx.core.graphics.ColorUtils.calculateContrast(
                                color(themed, com.google.android.material.R.attr.colorOnSurface), color(themed, attr)) >= 4.5);
                    }
                    var root = android.view.LayoutInflater.from(themed).inflate(R.layout.activity_login, null, false);
                    ((TextInputLayout)root.findViewById(R.id.emailLayout)).setError(context.getString(R.string.auth_invalid_email));
                    ((TextView)root.findViewById(R.id.formNotice)).setText(R.string.auth_network_error);
                    root.findViewById(R.id.formNotice).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.themePendingNotice).setVisibility(View.VISIBLE);
                    float density = themed.getResources().getDisplayMetrics().density;
                    int width = Math.round(size[0]*density), height = Math.round(size[1]*density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    root.layout(0,0,width,height);
                    bounds(root, width);
                    assertTrue(root.findViewById(R.id.loginContent).getWidth() <= 480*density);
                    assertTrue(root.findViewById(R.id.submitButton).getHeight() >= 56*density);
                }
        });
    }
    @Test public void changedPreferenceDefersDraftThenAppliesWhenEmptyWithoutSessionWrite() throws Exception {
        var file = new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        byte[] before = file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null;
        try (var screen = open()) {
            final LoginActivity[] original = new LoginActivity[1];
            screen.onActivity(activity -> {
                original[0] = activity;
                ((EditText)activity.findViewById(R.id.emailInput)).setText("draft@example.test");
                ((EditText)activity.findViewById(R.id.passwordInput)).setText("qa-password-draft");
            });
            screen.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            new ThemePreferences(context).save(AppTheme.SOFT, ColorMode.LIGHT);
            screen.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                assertSame(original[0], activity);
                assertEquals("qa-password-draft", ((EditText)activity.findViewById(R.id.passwordInput)).getText().toString());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.themePendingNotice).getVisibility());
                ((EditText)activity.findViewById(R.id.passwordInput)).setText("");
                ((EditText)activity.findViewById(R.id.emailInput)).setText("");
            });
            android.os.SystemClock.sleep(1200);
            screen.onActivity(activity -> {
                assertNotSame(original[0], activity);
                assertEquals(3, attribute(activity, R.attr.ttLoginKind));
                assertEquals(View.GONE, activity.findViewById(R.id.themePendingNotice).getVisibility());
            });
        }
        assertArrayEquals(before, file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null);
    }
    @Test public void loadingDefersThemeUntilIdle() throws Exception {
        try (var screen = open()) {
            final LoginActivity[] original = new LoginActivity[1];
            screen.onActivity(activity -> { original[0] = activity; activity.setLoading(true); });
            screen.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            new ThemePreferences(context).save(AppTheme.GLASS, ColorMode.DARK);
            screen.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> { assertSame(original[0], activity); activity.setLoading(false); });
            android.os.SystemClock.sleep(1200);
            screen.onActivity(activity -> {
                assertNotSame(original[0], activity);
                assertEquals(2, attribute(activity, R.attr.ttLoginKind));
            });
        }
    }
    private int attribute(Context themed, int attr) {
        var value = new android.util.TypedValue();
        assertTrue(themed.getTheme().resolveAttribute(attr, value, true)); return value.data;
    }
    private void bounds(View view, int width) {
        if (view.getVisibility() == View.GONE) return;
        assertTrue("Horizontal bounds: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof TextView text && !(text instanceof EditText) && text.getLayout() != null) {
            assertTrue("Text height", text.getLayout().getHeight() <= text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        }
        if (view instanceof android.view.ViewGroup group)
            for (int i=0; i<group.getChildCount(); i++) bounds(group.getChildAt(i), group.getWidth());
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(500);
        var runner = InstrumentationRegistry.getInstrumentation();
        var bitmap = runner.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var dir = new java.io.File(context.getExternalFilesDir(null), "artifacts/theme04");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try (var out = new java.io.FileOutputStream(new java.io.File(dir, name + ".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}
