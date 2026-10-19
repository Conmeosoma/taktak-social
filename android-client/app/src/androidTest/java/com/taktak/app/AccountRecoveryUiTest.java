package com.taktak.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.data.session.ResetSessionVault;
import com.taktak.app.ui.auth.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chỉ UI/fixture, không gửi email, đăng xuất hoặc reset tài khoản cá nhân. */
@RunWith(AndroidJUnit4.class)
public class AccountRecoveryUiTest {
    private Activity active;
    @After public void finish() { ResetSessionVault.clear(); if (active!=null) main(() -> active.finish()); }
    @Test public void forgotValidatesEmailAndLoadingWithoutNetwork() throws Exception {
        launch(ForgotPasswordActivity.class);
        main(() -> {
            active.findViewById(R.id.submit).performClick();
            assertNotNull(field(R.id.emailLayout).getError());
            text(R.id.emailInput).setText("bad-email"); active.findViewById(R.id.submit).performClick();
            assertEquals(active.getString(R.string.auth_invalid_email),field(R.id.emailLayout).getError());
            ((AuthFormActivity)active).setLoading(true); assertFalse(active.findViewById(R.id.submit).isEnabled());
            assertFalse(text(R.id.emailInput).isEnabled());
            ((AuthFormActivity)active).setLoading(false); assertTrue(active.findViewById(R.id.submit).isEnabled());
        }); capture("forgot-validation.png");
    }
    @Test public void verificationRequiresPasswordAndCodeAndDoesNotSaveCredentials() throws Exception {
        launch(EmailVerificationActivity.class);
        main(() -> {
            active.findViewById(R.id.confirmCode).performClick();
            assertNotNull(field(R.id.emailLayout).getError()); assertNotNull(field(R.id.currentLayout).getError());
            assertNotNull(field(R.id.codeLayout).getError());
            assertFalse(text(R.id.currentInput).isSaveEnabled()); assertFalse(text(R.id.codeInput).isSaveEnabled());
            text(R.id.currentInput).setText("QA-fixture-only");
            var toggle=field(R.id.currentLayout).findViewById(com.google.android.material.R.id.text_input_end_icon);
            toggle.performClick(); assertNull(text(R.id.currentInput).getTransformationMethod());
            toggle.performClick(); assertNotNull(text(R.id.currentInput).getTransformationMethod()); text(R.id.currentInput).setText("");
        }); capture("verify-validation.png");
    }
    @Test public void changePasswordCannotRunWithoutResetSession() throws Exception {
        ResetSessionVault.clear(); launch(ChangePasswordActivity.class);
        main(() -> {
            assertFalse(active.findViewById(R.id.submit).isEnabled());
            assertEquals(View.VISIBLE,active.findViewById(R.id.notice).getVisibility());
        }); capture("change-expired.png");
    }
    @Test public void changePasswordValidatesConfirmationAndUsesOnlyMemorySession() throws Exception {
        ResetSessionVault.put("a".repeat(43),900); launch(ChangePasswordActivity.class);
        main(() -> {
            text(R.id.currentInput).setText("a".repeat(43)); text(R.id.newInput).setText("QA-new-fixture-password");
            text(R.id.confirmInput).setText("mismatch"); active.findViewById(R.id.submit).performClick();
            assertNotNull(field(R.id.confirmLayout).getError());
            assertFalse(text(R.id.newInput).isSaveEnabled()); assertFalse(text(R.id.confirmInput).isSaveEnabled());
            text(R.id.newInput).setText("a".repeat(43)); active.findViewById(R.id.submit).performClick();
            assertEquals(active.getString(R.string.recovery_different),field(R.id.newLayout).getError());
            text(R.id.currentInput).setText(""); text(R.id.newInput).setText(""); text(R.id.confirmInput).setText("");
        }); capture("change-validation.png");
    }
    @Test public void recoveryLayoutSupportsBothThemesLargeFontAndNarrowLandscape() {
        main(() -> {
            var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            for(int night : new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
                for(int[] size : new int[][]{{280,560},{360,720},{840,420}}) {
                    var config=new Configuration(context.getResources().getConfiguration()); config.fontScale=1.5f;
                    config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|night;
                    var themed=new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),R.style.Theme_TakTak_Auth);
                    float density=themed.getResources().getDisplayMetrics().density;
                    View root=android.view.LayoutInflater.from(themed).inflate(R.layout.activity_recovery_form,null,false);
                    ((android.widget.TextView)root.findViewById(R.id.authTitle)).setText(R.string.recovery_change_title);
                    root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(size[0]*density),View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(Math.round(size[1]*density),View.MeasureSpec.EXACTLY));
                    root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight()); bounds(root,root.getWidth());
                    assertTrue(root.findViewById(R.id.submit).getHeight()>=48*density);
                }
            }
        });
    }
    @Test public void changePasswordConfirmationRemainsReachableWithKeyboard() throws Exception {
        ResetSessionVault.put("a".repeat(43),900); launch(ChangePasswordActivity.class);
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.confirmInput))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo(),androidx.test.espresso.action.ViewActions.click());
        var shown=new java.util.concurrent.atomic.AtomicBoolean();
        long deadline=android.os.SystemClock.elapsedRealtime()+10000;
        while(!shown.get() && android.os.SystemClock.elapsedRealtime()<deadline) {
            main(() -> {
                var root=active.findViewById(android.R.id.content);
                var insets=androidx.core.view.ViewCompat.getRootWindowInsets(root);
                shown.set(insets!=null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
            }); android.os.SystemClock.sleep(100);
        }
        assertTrue("Physical keyboard visible",shown.get());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.confirmInput))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
        capture("change-keyboard.png");
        androidx.test.espresso.Espresso.closeSoftKeyboard();
        android.os.SystemClock.sleep(700);
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.submit))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
    }
    private void bounds(View view,int width) {
        if(view.getVisibility()==View.GONE) return;
        assertTrue(view.getLeft()>=0 && view.getRight()<=width);
        if(view instanceof android.view.ViewGroup group) for(int i=0;i<group.getChildCount();i++) bounds(group.getChildAt(i),group.getWidth());
    }
    private void launch(Class<? extends Activity> type) throws Exception {
        var runner=InstrumentationRegistry.getInstrumentation();
        try(var descriptor=runner.getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
            var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) { while(stream.read(new byte[1024])!=-1) { } }
        main(() -> runner.getTargetContext().startActivity(new Intent(runner.getTargetContext(),type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        long deadline=android.os.SystemClock.elapsedRealtime()+12000;
        while(active==null && android.os.SystemClock.elapsedRealtime()<deadline) {
            main(() -> { for(var activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) if(type.isInstance(activity)) active=activity; });
            android.os.SystemClock.sleep(100);
        }
        assertNotNull(active);
    }
    private EditText text(int id) { return active.findViewById(id); }
    private TextInputLayout field(int id) { return active.findViewById(id); }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(700);
        var runner=InstrumentationRegistry.getInstrumentation(); var screenshot=runner.getUiAutomation().takeScreenshot(); assertNotNull(screenshot);
        var directory=new java.io.File(runner.getTargetContext().getExternalFilesDir(null),"artifacts/account-recovery");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try(var output=new java.io.FileOutputStream(new java.io.File(directory,name))) { assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)); }
        finally { screenshot.recycle(); }
    }
}
