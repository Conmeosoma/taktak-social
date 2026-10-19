package com.taktak.app.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.TypedValue;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chỉ dùng file QA riêng, không đọc/ghi phiên hoặc lựa chọn theme thật của người dùng. */
@RunWith(AndroidJUnit4.class)
public class ThemePreferencesDeviceTest {
    private Context context;
    private String file;
    private String sessionFile;
    private SharedPreferences preferences;

    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        file = "qa_appearance_" + UUID.randomUUID();
        sessionFile = "qa_session_" + UUID.randomUUID();
        preferences = context.getSharedPreferences(file, Context.MODE_PRIVATE);
    }

    @After public void cleanup() {
        context.deleteSharedPreferences(file);
        context.deleteSharedPreferences(sessionFile);
    }

    @Test public void realPreferencesPersistEveryStyleAndModeAcrossRepositoryReopen() throws Exception {
        ThemeRepository repository = new ThemeRepository(new ThemePreferences(preferences));
        for (AppTheme theme : AppTheme.values()) {
            for (ColorMode mode : ColorMode.values()) {
                ThemeSelection result = repository.save(theme, mode);
                var reopened = new ThemePreferences(context.getSharedPreferences(file, Context.MODE_PRIVATE));
                assertEquals(result, reopened.load());
                if (result.revision() > 0) {
                    assertEquals(theme.id(), preferences.getString(ThemePreferences.STYLE, null));
                    assertEquals(mode.id(), preferences.getString(ThemePreferences.MODE, null));
                }
            }
        }
    }

    @Test public void qaLogoutAndThemeWritesAreIsolatedInBothDirections() throws Exception {
        SharedPreferences qaSession = context.getSharedPreferences(sessionFile, Context.MODE_PRIVATE);
        assertNotEquals("auth_session_v1", ThemePreferences.FILE_NAME);
        assertNotEquals("taktak_device_identity", ThemePreferences.FILE_NAME);
        assertTrue(qaSession.edit().putString("sentinel", "not-a-real-session").commit());
        var store = new ThemePreferences(preferences);
        ThemeSelection result = store.save(AppTheme.GLASS, ColorMode.DARK);
        assertEquals("not-a-real-session", qaSession.getString("sentinel", null));
        assertTrue(qaSession.edit().clear().commit());
        assertEquals(result, new ThemePreferences(preferences).load());
    }

    @Test public void wrongTypesAndFutureSchemaFallbackWithoutDestructiveMigration() throws Exception {
        var store = new ThemePreferences(preferences);
        store.save(AppTheme.SOFT, ColorMode.DARK);
        assertTrue(preferences.edit().putBoolean(ThemePreferences.STYLE, true).commit());
        var before = preferences.getAll();
        assertEquals(ThemeSelection.defaults(), store.load());
        assertEquals(before, preferences.getAll());
        assertTrue(preferences.edit().putInt(ThemePreferences.SCHEMA, 100).commit());
        assertThrows(java.io.IOException.class, () -> store.save(AppTheme.GRADIENT, ColorMode.LIGHT));
        assertEquals(100, preferences.getInt(ThemePreferences.SCHEMA, 0));
    }

    @Test public void resolvedOriginalThemesHaveMaterialAttributesInBothModes() {
        for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
            var config = new Configuration(context.getResources().getConfiguration());
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
            for (AppTheme style : AppTheme.values()) {
                for (ThemeManager.ScreenRole role : ThemeManager.ScreenRole.values()) {
                    var result = ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), role);
                    var themed = new ContextThemeWrapper(context.createConfigurationContext(config), result.themeResource());
                    assertTrue(themed.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface,
                            new TypedValue(), true));
                    assertTrue(result.requestedStyleAvailable());
                }
            }
        }
    }
}
