package com.taktak.app.ui.theme;

/** ID lưu trên disk không phụ thuộc thứ tự enum hoặc tên hiển thị. */
public enum AppTheme {
    ORIGINAL("original"), GRADIENT("gradient"), GLASS("glass"), SOFT("soft");

    private final String id;
    AppTheme(String id) { this.id = id; }
    public String id() { return id; }

    public static AppTheme fromId(String id) {
        for (AppTheme theme : values()) {
            if (theme.id.equals(id)) { return theme; }
        }
        return ORIGINAL;
    }
}
