package com.taktak.app.ui.theme;

import java.util.Objects;

public record ThemeSelection(AppTheme style, ColorMode colorMode, long revision) {
    public ThemeSelection {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(colorMode, "colorMode");
        if (revision < 0) { throw new IllegalArgumentException("Negative revision"); }
    }

    public static ThemeSelection defaults() {
        return new ThemeSelection(AppTheme.ORIGINAL, ColorMode.SYSTEM, 0);
    }
}
