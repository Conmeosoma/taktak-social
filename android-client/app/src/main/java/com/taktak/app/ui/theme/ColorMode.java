package com.taktak.app.ui.theme;

public enum ColorMode {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    private final String id;
    ColorMode(String id) { this.id = id; }
    public String id() { return id; }

    public static ColorMode fromId(String id) {
        for (ColorMode mode : values()) {
            if (mode.id.equals(id)) { return mode; }
        }
        return SYSTEM;
    }
}
