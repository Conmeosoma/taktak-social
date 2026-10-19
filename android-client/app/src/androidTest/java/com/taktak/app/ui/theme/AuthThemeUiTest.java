package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.R;
import com.taktak.app.data.session.ResetSessionVault;
import com.taktak.app.ui.auth.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/** QA opt-in USB: chỉ appearance được đổi tạm/restore, mọi form luôn có input không hợp lệ. */
@RunWith(AndroidJUnit4.class)
public class AuthThemeUiTest {
    private Context context;
    private Map<String, ?> original;
    private Activity active;
    private boolean foregroundReady;
    private boolean ownsResetFixture;
    private byte[] sessionBefore;
    private java.io.File sessionFile;
    @Before public void prepare() throws Exception {
        assertEquals("USB appearance QA requires explicit runner argument", "true",
                InstrumentationRegistry.getArguments().getString("taktak.allowAppearanceQa"));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        original = new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).getAll());
        sessionFile = new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        sessionBefore = sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null;
        assertNull("Do not replace a pending reset", ResetSessionVault.peek());
    }
    @After public void cleanup() throws Exception {
        close();
        if (context == null || original == null) return;
        if (ownsResetFixture) ResetSessionVault.clear();
        var editor = context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).edit();
        for (String key : new String[]{ThemePreferences.STYLE,ThemePreferences.MODE,ThemePreferences.SCHEMA,ThemePreferences.REVISION}) {
            if (!original.containsKey(key)) editor.remove(key);
            else {
                Object value = original.get(key);
                if (value instanceof String s) editor.putString(key,s);
                else if (value instanceof Integer i) editor.putInt(key,i);
                else if (value instanceof Long l) editor.putLong(key,l);
                else if (value instanceof Boolean b) editor.putBoolean(key,b);
                else if (value instanceof Float f) editor.putFloat(key,f);
                else if (value instanceof Set<?> s) editor.putStringSet(key, new HashSet<>((Set<String>)s));
                else throw new AssertionError("Unsupported preference type");
            }
        }
        assertTrue("Restore appearance", editor.commit());
        assertEquals(original, context.getSharedPreferences(ThemePreferences.FILE_NAME,Context.MODE_PRIVATE).getAll());
        assertArrayEquals("Encrypted session unchanged",sessionBefore,sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null);
    }
    @Test public void fourFormsFourStylesBothModesValidateLoadAndKeepSecretsVolatile() throws Exception {
        for (AppTheme style : AppTheme.values()) for (ColorMode mode : new ColorMode[]{ColorMode.LIGHT,ColorMode.DARK}) {
            new ThemePreferences(context).save(style,mode);
            for (Class<? extends Activity> type : List.of(RegisterActivity.class,ForgotPasswordActivity.class,
                    EmailVerificationActivity.class,ChangePasswordActivity.class)) {
                launch(type);
                capture(style.id()+"-"+mode.id()+"-"+type.getSimpleName());
                main(() -> {
                    assertEquals(mode == ColorMode.DARK ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO,
                            active.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
                    if (type == RegisterActivity.class) {
                        active.findViewById(R.id.submitButton).performClick();
                        for (int field : new int[]{R.id.displayNameLayout,R.id.usernameLayout,R.id.emailLayout,R.id.passwordLayout})
                            assertNotNull(((TextInputLayout)active.findViewById(field)).getError());
                        for (int input : new int[]{R.id.passwordInput,R.id.confirmPasswordInput}) assertFalse(active.findViewById(input).isSaveEnabled());
                    } else if (type == ChangePasswordActivity.class) {
                        assertFalse(active.findViewById(R.id.submit).isEnabled());
                        assertEquals(active.getString(R.string.recovery_expired), ((TextView)active.findViewById(R.id.notice)).getText().toString());
                    } else {
                        active.findViewById(type == EmailVerificationActivity.class ? R.id.confirmCode : R.id.submit).performClick();
                        assertNotNull(((TextInputLayout)active.findViewById(R.id.emailLayout)).getError());
                        if (type == EmailVerificationActivity.class) {
                            assertNotNull(((TextInputLayout)active.findViewById(R.id.currentLayout)).getError());
                            assertNotNull(((TextInputLayout)active.findViewById(R.id.codeLayout)).getError());
                        }
                    }
                    var form = (AuthFormActivity)active;
                    form.setLoading(true);
                    assertFalse(active.findViewById(type == RegisterActivity.class ? R.id.submitButton : R.id.submit).isEnabled());
                    form.setLoading(false);
                    if (type == ChangePasswordActivity.class) assertFalse(active.findViewById(R.id.submit).isEnabled());
                    for (int input : type == RegisterActivity.class ? new int[]{R.id.passwordInput,R.id.confirmPasswordInput}
                            : new int[]{R.id.currentInput,R.id.codeInput,R.id.newInput,R.id.confirmInput}) assertFalse(active.findViewById(input).isSaveEnabled());
                });
                close();
            }
        }
    }
    @Test public void resetFixtureRejectsMismatchAndSamePasswordWithoutNetworkAndRetainsEye() throws Exception {
        for (AppTheme style : AppTheme.values()) {
            new ThemePreferences(context).save(style,ColorMode.SYSTEM);
            ownsResetFixture = true;
            ResetSessionVault.put("a".repeat(43),900);
            launch(ChangePasswordActivity.class);
            main(() -> {
                ((EditText)active.findViewById(R.id.currentInput)).setText("a".repeat(43));
                ((EditText)active.findViewById(R.id.newInput)).setText("qa-new-password");
                ((EditText)active.findViewById(R.id.confirmInput)).setText("mismatch");
                active.findViewById(R.id.submit).performClick();
                assertNotNull(((TextInputLayout)active.findViewById(R.id.confirmLayout)).getError());
                var field = (TextInputLayout)active.findViewById(R.id.newLayout);
                field.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
                assertNull(((EditText)active.findViewById(R.id.newInput)).getTransformationMethod());
                field.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
                assertNotNull(((EditText)active.findViewById(R.id.newInput)).getTransformationMethod());
                ((EditText)active.findViewById(R.id.newInput)).setText("a".repeat(43));
                active.findViewById(R.id.submit).performClick();
                assertEquals(active.getString(R.string.recovery_different),field.getError());
            });
            capture(style.id()+"-reset-validation");
            close(); ResetSessionVault.clear(); ownsResetFixture = false;
        }
    }
    @Test public void registerAndChangeConfirmationReachableWithPhysicalIme() throws Exception {
        new ThemePreferences(context).save(AppTheme.GLASS,ColorMode.LIGHT);
        for (Class<? extends Activity> type : List.of(RegisterActivity.class,ChangePasswordActivity.class)) {
            launch(type);
            int field = type == RegisterActivity.class ? R.id.confirmPasswordInput : R.id.confirmInput;
            int cta = type == RegisterActivity.class ? R.id.submitButton : R.id.submit;
            onView(withId(field)).perform(scrollTo(),click());
            main(() -> androidx.core.view.WindowCompat.getInsetsController(active.getWindow(),
                    active.findViewById(android.R.id.content)).show(androidx.core.view.WindowInsetsCompat.Type.ime()));
            var shown = new java.util.concurrent.atomic.AtomicBoolean();
            long until = android.os.SystemClock.uptimeMillis()+10000;
            while (!shown.get() && android.os.SystemClock.uptimeMillis()<until) {
                main(() -> {
                    var insets = androidx.core.view.ViewCompat.getRootWindowInsets(active.findViewById(android.R.id.content));
                    shown.set(insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
                }); android.os.SystemClock.sleep(100);
            }
            assertTrue("Physical IME shown",shown.get());
            onView(withId(field)).perform(scrollTo()).check(matches(isDisplayed()));
            capture(type.getSimpleName()+"-ime");
            androidx.test.espresso.Espresso.closeSoftKeyboard();
            onView(withId(cta)).perform(scrollTo()).check(matches(isDisplayed()));
            close();
        }
    }
    @Test public void fourStylesLayoutFontAndContrastMatrix() {
        main(() -> {
            for (AppTheme style : AppTheme.values()) for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for (float scale : new float[]{1f,1.5f,2f}) for (int[] size : new int[][]{{280,560},{360,720},{840,420}})
                    for (int layout : new int[]{R.layout.activity_register,R.layout.activity_recovery_form}) {
                        var config = new Configuration(context.getResources().getConfiguration()); config.fontScale=scale;
                        config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|night;
                        var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),
                                ThemeManager.resolve(new ThemeSelection(style,ColorMode.SYSTEM,0),ThemeManager.ScreenRole.AUTH).themeResource());
                        View root=LayoutInflater.from(themed).inflate(layout,null,false);
                        ((TextView)root.findViewById(R.id.authTitle)).setText(R.string.recovery_change_title);
                        if (layout == R.layout.activity_recovery_form) {
                            ((TextView)root.findViewById(R.id.notice)).setText(R.string.auth_network_error);
                            root.findViewById(R.id.notice).setVisibility(View.VISIBLE);
                        }
                        float density=themed.getResources().getDisplayMetrics().density;
                        int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                        root.layout(0,0,w,h); bounds(root,w);
                        int ink=com.google.android.material.color.MaterialColors.getColor(themed,com.google.android.material.R.attr.colorOnSurface,"QA");
                        int bg=com.google.android.material.color.MaterialColors.getColor(themed,com.google.android.material.R.attr.colorSurface,"QA");
                        assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(ink,bg)>=4.5);
                        if (style != AppTheme.ORIGINAL) {
                            int field = com.google.android.material.color.MaterialColors.getColor(themed,R.attr.ttLoginField,"QA");
                            int plate = androidx.core.graphics.ColorUtils.compositeColors(field,bg);
                            for (int id : layout == R.layout.activity_register
                                    ? new int[]{R.id.displayNameInput,R.id.usernameInput,R.id.emailInput,R.id.passwordInput,R.id.confirmPasswordInput}
                                    : new int[]{R.id.emailInput,R.id.currentInput,R.id.codeInput,R.id.newInput,R.id.confirmInput}) {
                                var input = (EditText)root.findViewById(id);
                                assertTrue("Input hint contrast >=4.5",androidx.core.graphics.ColorUtils.calculateContrast(input.getCurrentHintTextColor(),plate)>=4.5);
                                assertTrue("Input text contrast >=4.5",androidx.core.graphics.ColorUtils.calculateContrast(input.getCurrentTextColor(),plate)>=4.5);
                            }
                        }
                    }
        });
    }
    private void bounds(View v,int width) {
        if(v.getVisibility()==View.GONE) return;
        assertTrue("Horizontal bounds "+v.getClass().getSimpleName(),v.getLeft()>=0 && v.getRight()<=width);
        if(v instanceof TextView t && !(t instanceof EditText) && t.getLayout()!=null)
            assertTrue("Text fits height",t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());
        if(v instanceof android.view.ViewGroup g) for(int i=0;i<g.getChildCount();i++) bounds(g.getChildAt(i),g.getWidth());
    }
    private void launch(Class<? extends Activity> type) throws Exception {
        close(); var runner=InstrumentationRegistry.getInstrumentation();
        if (!foregroundReady) {
            // MIUI chỉ cho mở form sau khi launcher thực đã được đưa lên foreground.
            try (var descriptor = runner.getUiAutomation().executeShellCommand(
                    "am start -W -n com.taktak.app/.MainActivity");
                 var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
            }
            foregroundReady = true;
        }
        active=runner.startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runner.waitForIdleSync();
        long until=android.os.SystemClock.uptimeMillis()+10000;
        var focus=new java.util.concurrent.atomic.AtomicBoolean();
        while(!focus.get() && android.os.SystemClock.uptimeMillis()<until) {
            main(() -> focus.set(active.getWindow().getDecorView().hasWindowFocus())); android.os.SystemClock.sleep(100);
        }
        assertTrue("Auth screen has foreground focus",focus.get());
    }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void close() { if(active!=null) { Activity closing=active; main(closing::finish); active=null; InstrumentationRegistry.getInstrumentation().waitForIdleSync(); } }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(400);
        var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var dir=new java.io.File(context.getExternalFilesDir(null),"artifacts/theme05"); assertTrue(dir.isDirectory() || dir.mkdirs());
        try(var out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        } finally { bitmap.recycle(); }
    }
}
