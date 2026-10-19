package com.taktak.app.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.WorkerThread;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/** Cấu hình không nhạy cảm; không mở hoặc clear bất kỳ file phiên nào. */
public final class ThemePreferences implements ThemeStore {
    static final String FILE_NAME = "taktak_appearance_v1";
    static final String STYLE = "style_id";
    static final String MODE = "color_mode";
    static final String SCHEMA = "schema_version";
    static final String REVISION = "revision";
    private static final int VERSION = 1;
    private static final Object LOCK = new Object();
    private final SharedPreferences preferences;

    public ThemePreferences(Context context) {
        this(context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE));
    }

    ThemePreferences(SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override public ThemeSelection load() {
        synchronized (LOCK) { return decode(preferences.getAll()); }
    }

    private static ThemeSelection decode(Map<String, ?> values) {
        if (!(values.get(SCHEMA) instanceof Integer version) || version != VERSION
                || !(values.get(STYLE) instanceof String style)
                || !(values.get(MODE) instanceof String mode)
                || !(values.get(REVISION) instanceof Long revision) || revision < 0) {
            return ThemeSelection.defaults();
        }
        AppTheme theme = AppTheme.fromId(style);
        ColorMode colorMode = ColorMode.fromId(mode);
        if (!theme.id().equals(style) || !colorMode.id().equals(mode)) {
            return ThemeSelection.defaults();
        }
        return new ThemeSelection(theme, colorMode, revision);
    }

    @WorkerThread
    @Override public ThemeSelection save(AppTheme style, ColorMode mode) throws IOException {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(mode, "mode");
        synchronized (LOCK) {
            Map<String, ?> before = preferences.getAll();
            if (before.get(SCHEMA) instanceof Integer version && version > VERSION) {
                throw new IOException("Unsupported appearance schema");
            }
            ThemeSelection current = decode(before);
            if (current.style() == style && current.colorMode() == mode) { return current; }
            if (current.revision() == Long.MAX_VALUE) { throw new IOException("Appearance revision exhausted"); }
            ThemeSelection next = new ThemeSelection(style, mode, current.revision() + 1);
            boolean saved = preferences.edit().putInt(SCHEMA, VERSION)
                    .putString(STYLE, style.id()).putString(MODE, mode.id())
                    .putLong(REVISION, next.revision()).commit();
            if (!saved) {
                // commit thất bại vẫn cập nhật cache Android: khôi phục đúng các key sở hữu.
                SharedPreferences.Editor rollback = preferences.edit();
                for (String key : new String[]{STYLE, MODE, SCHEMA, REVISION}) {
                    Object value = before.get(key);
                    if (value instanceof String text) { rollback.putString(key, text); }
                    else if (value instanceof Integer number) { rollback.putInt(key, number); }
                    else if (value instanceof Long number) { rollback.putLong(key, number); }
                    else if (value instanceof Boolean flag) { rollback.putBoolean(key, flag); }
                    else if (value instanceof Float number) { rollback.putFloat(key, number); }
                    else if (value instanceof Set<?> items) {
                        Set<String> restoredItems = new HashSet<>();
                        for (Object item : items) {
                            if (item instanceof String text) { restoredItems.add(text); }
                        }
                        rollback.putStringSet(key, restoredItems);
                    }
                    else { rollback.remove(key); }
                }
                boolean restored = rollback.commit();
                throw new IOException(restored ? "Appearance save failed" : "Appearance save and rollback failed");
            }
            return next;
        }
    }
}
