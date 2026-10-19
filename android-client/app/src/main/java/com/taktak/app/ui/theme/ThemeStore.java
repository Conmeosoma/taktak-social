package com.taktak.app.ui.theme;

import androidx.annotation.WorkerThread;
import java.io.IOException;

public interface ThemeStore {
    ThemeSelection load();

    @WorkerThread
    ThemeSelection save(AppTheme style, ColorMode mode) throws IOException;
}
