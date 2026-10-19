package com.taktak.app.ui.theme;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.R;
import com.taktak.app.ui.auth.LoginActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThemeSelectionUiTest {
    private Context context;
    @Before public void setup() {
        assertTrue("Only run theme mutation QA on emulator", android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.HARDWARE.contains("goldfish"));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // File appearance không nhạy cảm của AVD read-only dành riêng lượt QA.
        context.deleteSharedPreferences(ThemePreferences.FILE_NAME);
    }
    @After public void cleanup() { if (context != null) { context.deleteSharedPreferences(ThemePreferences.FILE_NAME); } }
    private ActivityScenario<ThemeSettingsActivity> open() {
        return ActivityScenario.launch(new Intent(context, ThemeSettingsActivity.class));
    }
    private void select(ThemeSettingsActivity activity, AppTheme style, int modeId) {
        activity.findViewById(R.id.themeOptions).findViewWithTag(style.id()).performClick();
        ((android.widget.RadioGroup) activity.findViewById(R.id.themeModes)).check(modeId);
    }
    private void awaitSaved(AppTheme style, ColorMode mode) throws Exception {
        long until = android.os.SystemClock.uptimeMillis() + 6000;
        while (android.os.SystemClock.uptimeMillis() < until) {
            var selection = new ThemePreferences(context).load();
            if (selection.style() == style && selection.colorMode() == mode) {
                android.os.SystemClock.sleep(700); return;
            }
            android.os.SystemClock.sleep(30);
        }
        fail("Appearance not persisted");
    }
    @Test public void cancelAndBackDoNotPersistPreview() {
        try (var scenario = open()) {
            scenario.onActivity(activity -> {
                select(activity, AppTheme.GLASS, R.id.themeDark);
                assertEquals(ThemeSelection.defaults(), new ThemePreferences(context).load());
                activity.findViewById(R.id.themeCancel).performClick();
            });
        }
        try (var scenario = open()) {
            scenario.onActivity(activity -> {
                select(activity, AppTheme.SOFT, R.id.themeLight);
                activity.getOnBackPressedDispatcher().onBackPressed();
            });
        }
        assertEquals(ThemeSelection.defaults(), new ThemePreferences(context).load());
    }
    @Test public void applyRecreatesOnlyChooserAndReopenRestoresSelection() throws Exception {
        try (var scenario = open()) {
            scenario.onActivity(activity -> {
                select(activity, AppTheme.SOFT, R.id.themeDark);
                activity.findViewById(R.id.themeApply).performClick();
            });
            awaitSaved(AppTheme.SOFT, ColorMode.DARK);
            scenario.onActivity(activity -> {
                assertFalse(activity.findViewById(R.id.themeApply).isEnabled());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.themeNotice).getVisibility());
                assertTrue(((com.google.android.material.card.MaterialCardView) activity.findViewById(R.id.themeOptions)
                        .findViewWithTag("soft")).isChecked());
            });
            capture("soft-dark-applied.png");
        }
        try (var reopened = open()) {
            reopened.onActivity(activity -> assertTrue(((android.widget.RadioButton) activity.findViewById(R.id.themeDark)).isChecked()));
        }
    }
    @Test public void unsavedDraftSurvivesRecreationWithoutDiskWrite() {
        try (var scenario = open()) {
            scenario.onActivity(activity -> select(activity, AppTheme.GRADIENT, R.id.themeLight));
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertTrue(((com.google.android.material.card.MaterialCardView) activity.findViewById(R.id.themeOptions)
                        .findViewWithTag("gradient")).isChecked());
                assertTrue(((android.widget.RadioButton) activity.findViewById(R.id.themeLight)).isChecked());
                assertEquals(ThemeSelection.defaults(), new ThemePreferences(context).load());
            });
        }
    }
    @Test public void saveErrorPreservesDraftAndRetryWorks() throws Exception {
        assertTrue(context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE)
                .edit().putInt(ThemePreferences.SCHEMA, 99).commit());
        try (var scenario = open()) {
            scenario.onActivity(activity -> {
                select(activity, AppTheme.GLASS, R.id.themeDark);
                activity.findViewById(R.id.themeApply).performClick();
            });
            android.os.SystemClock.sleep(500);
            scenario.onActivity(activity -> {
                assertEquals(activity.getString(R.string.theme_save_error),
                        ((android.widget.TextView) activity.findViewById(R.id.themeNotice)).getText().toString());
                assertTrue(activity.findViewById(R.id.themeApply).isEnabled());
                assertEquals(ThemeSelection.defaults(), new ThemePreferences(context).load());
            });
            capture("save-error.png");
            // Gỡ blocker chỉ trong fixture AVD; không phá schema production để giả lỗi IO.
            assertTrue(context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(ThemePreferences.SCHEMA, 1).putString(ThemePreferences.STYLE, "original")
                    .putString(ThemePreferences.MODE, "system").putLong(ThemePreferences.REVISION, 0).commit());
            scenario.onActivity(activity -> activity.findViewById(R.id.themeApply).performClick());
            awaitSaved(AppTheme.GLASS, ColorMode.DARK);
        }
    }
    @Test public void everyStyleLightDarkAndSystemRemainSelectable() throws Exception {
        for (AppTheme style : AppTheme.values()) {
            for (ColorMode mode : new ColorMode[]{ColorMode.LIGHT, ColorMode.DARK}) {
                try (var scenario = open()) {
                    scenario.onActivity(activity -> {
                        select(activity, style, mode == ColorMode.LIGHT ? R.id.themeLight : R.id.themeDark);
                        activity.findViewById(R.id.themeApply).performClick();
                    });
                    awaitSaved(style, mode);
                    capture(style.id() + "-" + mode.id() + ".png");
                }
            }
        }
        try (var scenario = open()) {
            scenario.onActivity(activity -> { select(activity, AppTheme.SOFT, R.id.themeSystem); activity.findViewById(R.id.themeApply).performClick(); });
            awaitSaved(AppTheme.SOFT, ColorMode.SYSTEM);
        }
    }
    @Test public void loginEntryReturnsToSameFormWithoutRecreationOrSessionWrite() throws Exception {
        var file = new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        byte[] before = file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null;
        try (var scenario = ActivityScenario.<LoginActivity>launch(new Intent(context, LoginActivity.class))) {
            final LoginActivity[] original = new LoginActivity[1];
            scenario.onActivity(activity -> {
                original[0] = activity;
                ((android.widget.EditText) activity.findViewById(R.id.emailInput)).setText("draft@example.test");
                ((android.widget.EditText) activity.findViewById(R.id.passwordInput)).setText("local-draft-only");
                activity.findViewById(R.id.loginThemeButton).performClick();
            });
            android.os.SystemClock.sleep(600);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withTagValue(org.hamcrest.Matchers.is("glass")))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.themeApply))
                    .perform(androidx.test.espresso.action.ViewActions.click());
            awaitSaved(AppTheme.GLASS, ColorMode.SYSTEM);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.themeCancel))
                    .perform(androidx.test.espresso.action.ViewActions.click());
            scenario.onActivity(activity -> {
                assertSame(original[0], activity);
                assertEquals("draft@example.test", ((android.widget.EditText) activity.findViewById(R.id.emailInput)).getText().toString());
                assertEquals("local-draft-only", ((android.widget.EditText) activity.findViewById(R.id.passwordInput)).getText().toString());
            });
        }
        byte[] after = file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null;
        assertTrue("Encrypted session bytes unchanged", java.util.Arrays.equals(before, after));
    }
    @Test public void chooserLayoutsFitFontSizesAndHaveReadableTextColors() {
        var runner = InstrumentationRegistry.getInstrumentation();
        runner.runOnMainSync(() -> {
            for (AppTheme style : AppTheme.values()) {
                for (int night : new int[]{android.content.res.Configuration.UI_MODE_NIGHT_NO, android.content.res.Configuration.UI_MODE_NIGHT_YES}) {
                    for (float scale : new float[]{1f, 1.5f, 2f}) {
                        for (int[] size : new int[][]{{280,560},{360,720},{840,420}}) {
                            var config = new android.content.res.Configuration(context.getResources().getConfiguration());
                            config.fontScale = scale; config.uiMode = (config.uiMode & ~android.content.res.Configuration.UI_MODE_NIGHT_MASK) | night;
                            var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), ThemeManager.chooserThemeResource(style));
                            int bg = com.google.android.material.color.MaterialColors.getColor(themed, com.google.android.material.R.attr.colorSurface, "QA");
                            for (int attr : new int[]{com.google.android.material.R.attr.colorOnSurface, com.google.android.material.R.attr.colorOnSurfaceVariant}) {
                                int ink = com.google.android.material.color.MaterialColors.getColor(themed, attr, "QA");
                                assertTrue("Chooser text contrast", androidx.core.graphics.ColorUtils.calculateContrast(ink, bg) >= 4.5);
                            }
                            var root = android.view.LayoutInflater.from(themed).inflate(R.layout.activity_theme_settings, null, false);
                            var options = (android.widget.LinearLayout) root.findViewById(R.id.themeOptions);
                            for (int title : new int[]{R.string.theme_original, R.string.theme_gradient, R.string.theme_glass, R.string.theme_soft}) {
                                var item = android.view.LayoutInflater.from(themed).inflate(R.layout.item_theme_option, options, false);
                                ((android.widget.TextView) item.findViewById(R.id.themeOptionTitle)).setText(title);
                                ((android.widget.TextView) item.findViewById(R.id.themeOptionDetail)).setText(R.string.theme_gradient_detail);
                                options.addView(item);
                            }
                            ((android.widget.TextView) root.findViewById(R.id.themeNotice)).setText(R.string.theme_save_error);
                            root.findViewById(R.id.themeNotice).setVisibility(View.VISIBLE);
                            float density = themed.getResources().getDisplayMetrics().density;
                            int width = Math.round(size[0]*density), height = Math.round(size[1]*density);
                            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                            root.layout(0,0,width,height);
                            checkBounds(root, width);
                            assertTrue(root.findViewById(R.id.themeApply).getHeight() >= 48*density);
                            assertTrue(root.findViewById(R.id.themeScroll).getHeight() > 0);
                        }
                    }
                }
            }
        });
    }
    private void checkBounds(View view, int width) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof android.widget.TextView text && text.getLayout() != null) {
            assertTrue("No clipped text: " + text.getText(), text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof android.view.ViewGroup group) {
            for (int i=0;i<group.getChildCount();i++) { checkBounds(group.getChildAt(i), group.getWidth()); }
        }
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(500);
        var screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        var directory = new java.io.File(context.getExternalFilesDir(null), "artifacts/theme03");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { screenshot.recycle(); }
    }
}
