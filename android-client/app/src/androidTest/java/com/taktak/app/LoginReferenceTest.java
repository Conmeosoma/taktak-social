package com.taktak.app;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.taktak.app.ui.auth.LoginActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/** Kiểm tra các khác biệt có chủ đích của Login so với theme toàn ứng dụng. */
@RunWith(AndroidJUnit4.class)
public class LoginReferenceTest {
    @Test public void responsiveLargeFontDoesNotOverflow() {
        var runner = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        runner.runOnMainSync(() -> {
            var context = runner.getTargetContext();
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                for (int[] size : new int[][]{{280,560},{360,720},{840,420}}) {
                    var config = new Configuration(context.getResources().getConfiguration());
                    config.fontScale = 1.5f;
                    config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                    var theme = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                            R.style.Theme_TakTak_LoginReference);
                    float density = theme.getResources().getDisplayMetrics().density;
                    View root = android.view.LayoutInflater.from(theme).inflate(R.layout.activity_login, null, false);
                    ((com.google.android.material.textfield.TextInputLayout)root.findViewById(R.id.emailLayout))
                            .setError(context.getString(R.string.auth_invalid_email));
                    int width = Math.round(size[0]*density), height = Math.round(size[1]*density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                    root.layout(0,0,width,height);
                    checkBounds(root, width);
                    assertTrue(root.findViewById(R.id.loginContent).getWidth() <= Math.round(480*density));
                    for(int id : new int[]{R.id.submitButton,R.id.switchButton}) {
                        assertTrue(root.findViewById(id).getHeight() >= Math.round(56*density));
                    }
                }
            }
        });
    }
    private void checkBounds(View view, int parentWidth) {
        if(view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if(view instanceof TextView text && !(text instanceof android.widget.EditText) && text.getLayout() != null) {
            assertTrue("Text fits vertically: " + text.getClass().getSimpleName(), text.getLayout().getHeight()
                    <= text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        }
        if(view instanceof android.view.ViewGroup group) {
            for(int index=0;index<group.getChildCount();index++) { checkBounds(group.getChildAt(index),group.getWidth()); }
        }
    }
    @Test public void keyboardValidationAndRegisterNavigationWithoutChangingSession() throws Exception {
        try (var scenario = launchWithoutBootstrap()) {
            capture("login.png");
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.submitButton).performClick();
                assertNotNull(((com.google.android.material.textfield.TextInputLayout)
                        activity.findViewById(R.id.emailLayout)).getError());
                assertNotNull(((com.google.android.material.textfield.TextInputLayout)
                        activity.findViewById(R.id.passwordLayout)).getError());
            });
            capture("validation.png");
            onView(withId(R.id.passwordInput)).perform(scrollTo(), click());
            scenario.onActivity(activity -> androidx.core.view.ViewCompat.getWindowInsetsController(
                    activity.findViewById(android.R.id.content)).show(androidx.core.view.WindowInsetsCompat.Type.ime()));
            long deadline = android.os.SystemClock.elapsedRealtime() + 10000;
            var shown = new java.util.concurrent.atomic.AtomicBoolean();
            while (!shown.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    var root = activity.findViewById(android.R.id.content);
                    var insets = androidx.core.view.ViewCompat.getRootWindowInsets(root);
                    shown.set(insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
                });
                android.os.SystemClock.sleep(100);
            }
            assertTrue("Physical IME visible", shown.get());
            onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()));
            capture("keyboard-submit.png");
            androidx.test.espresso.Espresso.closeSoftKeyboard();
            shown.set(true);
            deadline = android.os.SystemClock.elapsedRealtime() + 10000;
            while (shown.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    var insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.findViewById(android.R.id.content));
                    shown.set(insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
                });
                android.os.SystemClock.sleep(100);
            }
            assertFalse("IME closed before moving to bottom CTA", shown.get());
            android.os.SystemClock.sleep(1000);
            capture("keyboard-closed.png");
            onView(withId(R.id.switchButton)).perform(scrollTo(), click());
            SessionRoutingTest.awaitView(R.id.displayNameInput);
            capture("register-navigation.png");
            onView(withId(R.id.switchButton)).perform(scrollTo(), click());
            SessionRoutingTest.awaitView(R.id.emailInput);
        }
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(800);
        var runner = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        var image = runner.getUiAutomation().takeScreenshot();
        assertNotNull(image);
        var directory = new java.io.File(runner.getTargetContext().getExternalFilesDir(null), "artifacts/login-reference-usb");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { image.recycle(); }
    }

    @Test public void passwordEyeTogglesBothWaysAfterLoading() throws Exception {
        try (var scenario = launchWithoutBootstrap()) {
            scenario.onActivity(activity -> {
                var password = (android.widget.EditText) activity.findViewById(R.id.passwordInput);
                activity.findViewById(R.id.submitButton).performClick();
                activity.setLoading(true);
                activity.setLoading(false);
                var layout = (com.google.android.material.textfield.TextInputLayout) activity.findViewById(R.id.passwordLayout);
                layout.setError(null);
                ((com.google.android.material.textfield.TextInputLayout) activity.findViewById(R.id.emailLayout)).setError(null);
                password.setText(java.util.UUID.randomUUID().toString());
                View eye = layout.findViewById(com.google.android.material.R.id.text_input_end_icon);
                assertTrue("Eye enabled after request completes", eye.isEnabled());
                assertNotNull("Password starts masked", password.getTransformationMethod());
                eye.performClick();
                assertNull("Eye reveals password", password.getTransformationMethod());
                eye.performClick();
                assertNotNull("Eye masks password again", password.getTransformationMethod());
            });
        }
    }
    @Test public void referenceHasRealRecoveryAndNoFakeLanguageAction() throws Exception {
        try (var scenario = launchWithoutBootstrap()) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.backButton).isClickable());
                assertFalse(activity.findViewById(R.id.languageLabel).isClickable());
                assertTrue(activity.findViewById(R.id.forgotPasswordLabel).isClickable());
                assertTrue(activity.findViewById(R.id.verifyEmailButton).isClickable());
                assertEquals(activity.getString(R.string.auth_email),
                        ((TextView) activity.findViewById(R.id.emailInput)).getHint().toString());
                assertEquals(activity.getString(R.string.login_create_account),
                        ((TextView) activity.findViewById(R.id.switchButton)).getText().toString());
                assertTrue(activity.findViewById(R.id.submitButton).getBackground() instanceof RippleDrawable);
                assertTrue(activity.findViewById(R.id.switchButton).getBackground() instanceof RippleDrawable);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.authLogo).getVisibility());
                assertEquals(Color.rgb(25,25,31), activity.getColor(R.color.login_background));
                activity.setLoading(true);
                assertFalse(activity.findViewById(R.id.submitButton).isEnabled());
                assertTrue(activity.findViewById(R.id.submitButton).getAlpha() < 1f);
                activity.setLoading(false);
            });
        }
    }

    // MIUI có thể chặn cửa sổ BootstrapActivity của ActivityScenario khi app ở nền.
    // Đưa launcher thật lên trước, rồi mở Login cùng UID; không sửa quyền hệ thống/phiên.
    private LoginScreen launchWithoutBootstrap() throws Exception {
        var runner = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        boolean emulator = android.os.Build.HARDWARE.contains("ranchu") || android.os.Build.HARDWARE.contains("goldfish");
        // AVD không cần bootstrap launcher: session routing bất đồng bộ có thể cướp focus Login mới.
        // Giữ workaround đưa app lên foreground cho MIUI/máy thật như trước.
        if (!emulator) {
            try (var descriptor = runner.getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
                 var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
            }
        }
        // startActivitySync trả đúng instance vừa mở; không bắt nhầm Login do launcher route.
        var launched = (LoginActivity) runner.startActivitySync(new android.content.Intent(
                runner.getTargetContext(), LoginActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        runner.waitForIdleSync();
        var focused = new java.util.concurrent.atomic.AtomicBoolean();
        long deadline = android.os.SystemClock.elapsedRealtime() + 12000;
        while (!focused.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
            runner.runOnMainSync(() -> focused.set(!launched.isFinishing()
                    && launched.getWindow().getDecorView().hasWindowFocus()));
            android.os.SystemClock.sleep(100);
        }
        assertTrue("Launched Login must have window focus without test bootstrap", focused.get());
        return new LoginScreen(launched);
    }
    private static final class LoginScreen implements AutoCloseable {
        private final LoginActivity activity;
        LoginScreen(LoginActivity activity) { this.activity = activity; }
        void onActivity(java.util.function.Consumer<LoginActivity> action) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> action.accept(activity));
        }
        @Override public void close() { onActivity(android.app.Activity::finish); }
    }
}
