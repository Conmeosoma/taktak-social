package com.taktak.app.ui.theme;

import androidx.annotation.WorkerThread;
import java.io.IOException;
import java.util.Objects;

/** Settings-only. Caller lưu trên worker; chỉ publish kết quả sau save thành công. */
public final class ThemeRepository {
    private final ThemeStore store;

    public ThemeRepository(ThemeStore store) { this.store = Objects.requireNonNull(store); }
    public ThemeSelection load() { return store.load(); }

    @WorkerThread
    public ThemeSelection save(AppTheme style, ColorMode mode) throws IOException {
        return store.save(style, mode);
    }
}
