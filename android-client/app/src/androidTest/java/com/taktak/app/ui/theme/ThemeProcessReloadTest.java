package com.taktak.app.ui.theme;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chạy phase write/read ở hai process instrumentation, chỉ dùng file QA cố định này. */
@RunWith(AndroidJUnit4.class)
public class ThemeProcessReloadTest {
    @Test public void selectionSurvivesProcessRestart() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String file = "qa_appearance_process_theme02";
        String phase = InstrumentationRegistry.getArguments().getString("themePhase", "roundTrip");
        if (!phase.equals("write") && !phase.equals("read") && !phase.equals("roundTrip")) {
            fail("Unknown QA phase");
        }
        try {
            var store = new ThemePreferences(context.getSharedPreferences(file, Context.MODE_PRIVATE));
            var expected = new ThemeSelection(AppTheme.GLASS, ColorMode.DARK, 1);
            if (!phase.equals("read")) {
                // Chỉ reset fixture QA của probe, không đụng file app/session thật.
                assertTrue(context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().commit());
                assertEquals(expected, store.save(AppTheme.GLASS, ColorMode.DARK));
            }
            assertEquals(expected, store.load());
        } finally {
            if (!phase.equals("write")) { assertTrue(context.deleteSharedPreferences(file)); }
        }
    }
}
