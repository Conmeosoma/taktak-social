package com.taktak.app.ui.splash;

import android.content.Context;

/** Cấu hình presentation riêng, không chạm JWT/session. */
public final class IntroPreferences {
    public static final String FILE = "startup_intro";
    private final android.content.SharedPreferences preferences;
    public IntroPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    public boolean enabled() { return preferences.getBoolean("enabled", true); }
    public boolean sound() { return preferences.getBoolean("sound", true); }
    public void enabled(boolean value) { preferences.edit().putBoolean("enabled", value).apply(); }
    public void sound(boolean value) { preferences.edit().putBoolean("sound", value).apply(); }
}
