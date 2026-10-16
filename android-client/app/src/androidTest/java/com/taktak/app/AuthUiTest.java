package com.taktak.app;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.ui.auth.AuthInputValidator;
import com.taktak.app.ui.auth.LoginActivity;
import com.taktak.app.ui.auth.RegisterActivity;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class AuthUiTest {
    @Before public void allowDebugLocalNetwork() {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            var instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
            instrumentation.getUiAutomation().grantRuntimePermission("com.taktak.app", "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }
    @Test public void launcherNavigatesLoginRegisterAndBackWithoutAuthenticating() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.loginButton)).perform(click());
            onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
            onView(withId(R.id.switchButton)).perform(scrollTo(), click());
            onView(withId(R.id.displayNameInput)).check(matches(isDisplayed()));
            onView(withId(R.id.switchButton)).perform(scrollTo(), click());
            onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
            onView(withId(R.id.bootstrapMessage)).check(matches(withText("Taktak")));
        }
    }

    @Test public void loginShowsRequiredAndInvalidEmailErrors() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.submitButton).performClick();
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.emailLayout)).getError());
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.passwordLayout)).getError());
                assertEquals(0, AuthInputValidator.emailError("  member@example.test  "));
                assertEquals(R.string.auth_invalid_email, AuthInputValidator.emailError("not-an-email"));
                assertEquals(R.string.auth_invalid_email, AuthInputValidator.emailError(new String(new char[255]).replace('\0', 'a') + "@example.test"));
                assertEquals(R.string.auth_required, AuthInputValidator.emailError("  "));
            });
        }
    }

    @Test public void registerValidatesThenCreatesRealBackendAccount() throws Exception {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            String fixture = java.util.UUID.randomUUID().toString();
            String username = "ui_" + java.util.UUID.randomUUID().toString().replace("-", "");
            scenario.onActivity(activity -> {
                input(activity, R.id.displayNameInput, "Tiến 📷");
                input(activity, R.id.usernameInput, "not.valid");
                input(activity, R.id.emailInput, username + "@example.test");
                input(activity, R.id.passwordInput, fixture);
                input(activity, R.id.confirmPasswordInput, fixture + " ");
                activity.findViewById(R.id.submitButton).performClick();
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.usernameLayout)).getError());
                assertNotNull(((TextInputLayout) activity.findViewById(R.id.confirmPasswordLayout)).getError());
                input(activity, R.id.usernameInput, "  " + username + "  ");
                input(activity, R.id.confirmPasswordInput, fixture);
                activity.findViewById(R.id.submitButton).performClick();
            });
            awaitNotice(scenario, R.string.auth_register_success);
            capture("register-success.png");
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.submitButton).isEnabled());
                assertNull(((TextInputLayout) activity.findViewById(R.id.usernameLayout)).getError());
                assertNull(((TextInputLayout) activity.findViewById(R.id.confirmPasswordLayout)).getError());
            });
        }
    }

    @Test public void loginLoadingDisablesInputsAndDuplicateSubmitUntilReleased() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setLoading(true);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.loadingIndicator).getVisibility());
                assertFalse(activity.findViewById(R.id.submitButton).isEnabled());
                assertFalse(activity.findViewById(R.id.switchButton).isEnabled());
                assertFalse(activity.findViewById(R.id.emailInput).isEnabled());
                activity.findViewById(R.id.submitButton).performClick();
                assertEquals(View.GONE, activity.findViewById(R.id.formNotice).getVisibility());
                activity.setLoading(false);
                assertTrue(activity.findViewById(R.id.submitButton).isEnabled());
                assertEquals(View.GONE, activity.findViewById(R.id.loadingIndicator).getVisibility());
            });
        }
    }

    @Test public void registerLoadingAndRecreationDoNotSavePasswords() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                input(activity, R.id.passwordInput, java.util.UUID.randomUUID().toString());
                input(activity, R.id.confirmPasswordInput, java.util.UUID.randomUUID().toString());
                activity.setLoading(true);
                assertFalse(activity.findViewById(R.id.submitButton).isEnabled());
                assertFalse(activity.findViewById(R.id.confirmPasswordInput).isEnabled());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.loadingIndicator).getVisibility());
                activity.setLoading(false);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
                assertEquals("", ((EditText) activity.findViewById(R.id.confirmPasswordInput)).getText().toString());
                assertTrue(activity.findViewById(R.id.submitButton).isEnabled());
            });
        }
    }

    @Test public void unknownLoginShowsRealErrorAndClearsPasswordOnRecreate() throws Exception {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                input(activity, R.id.emailInput, "member@example.test");
                input(activity, R.id.passwordInput, java.util.UUID.randomUUID().toString());
                activity.findViewById(R.id.submitButton).performClick();
            });
            awaitNotice(scenario, R.string.auth_invalid_credentials);
            capture("login-error.png");
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
                assertEquals("member@example.test", ((EditText) activity.findViewById(R.id.emailInput)).getText().toString());
            });
        }
    }

    @Test public void darkThemeRegisterStillScrollsToSubmit() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES));
            onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.submitButton)).perform(click());
            scenario.onActivity(activity -> assertNotNull(((TextInputLayout) activity.findViewById(R.id.displayNameLayout)).getError()));
        }
    }

    @Test public void confirmationFieldStaysAboveKeyboardWhenFocused() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.confirmPasswordInput)).perform(scrollTo(), click());
            // Chờ animation IME, không delay hoặc giả lập request mạng.
            android.os.SystemClock.sleep(1200);
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                var insets = androidx.core.view.ViewCompat.getRootWindowInsets(root);
                assertNotNull(insets);
                assertTrue(insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
                View confirmation = activity.findViewById(R.id.confirmPasswordInput);
                int[] position = new int[2];
                confirmation.getLocationOnScreen(position);
                int usableBottom = root.getHeight() - insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
                assertTrue("Focused password field must remain above IME", position[1] + confirmation.getHeight() <= usableBottom);
                assertTrue(confirmation.hasFocus());
            });
            androidx.test.espresso.Espresso.closeSoftKeyboard();
            onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test public void loginUsesViewModelAndSavesRealSessionWithoutCameraRouting() throws Exception {
        var context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        String username = "loginui_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String password = java.util.UUID.randomUUID().toString();
        var raw = new retrofit2.Retrofit.Builder().baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build()
                .create(com.taktak.app.data.remote.AuthApi.class);
        var created = raw.register(new com.taktak.app.data.model.AuthModels.RegisterRequest(username + "@example.test",
                username, "UI member", password)).execute();
        assertEquals(201, created.code());
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                input(activity, R.id.emailInput, username + "@example.test");
                input(activity, R.id.passwordInput, password);
                activity.findViewById(R.id.submitButton).performClick();
                activity.findViewById(R.id.submitButton).performClick();
            });
            awaitNotice(scenario, R.string.auth_login_success);
            capture("login-success.png");
            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                assertEquals("", ((EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
            });
            var store = new com.taktak.app.data.session.EncryptedSessionStore(context, BuildConfig.BASE_URL);
            assertNotNull(store.load());
            var restored = new com.taktak.app.data.remote.ApiClient(BuildConfig.BASE_URL, store);
            assertEquals(created.body().id, new com.taktak.app.data.repository.AuthRepository(restored).profile().id);
            restored.sessions.logout();
        }
    }

    private void input(android.app.Activity activity, int id, String value) {
        ((EditText) activity.findViewById(id)).setText(value);
    }

    private void awaitNotice(ActivityScenario<? extends android.app.Activity> scenario, int text) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 30000;
        java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean();
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> found.set(((android.widget.TextView) activity.findViewById(R.id.formNotice))
                    .getText().toString().equals(activity.getString(text))));
            if (found.get()) { return; }
            android.os.SystemClock.sleep(50);
        }
        fail("Expected authentication UI result did not arrive.");
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        android.graphics.Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        java.io.File directory = new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null), "artifacts/3b");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
