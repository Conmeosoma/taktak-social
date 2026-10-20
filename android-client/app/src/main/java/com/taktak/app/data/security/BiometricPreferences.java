package com.taktak.app.data.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

public final class BiometricPreferences {
    private static final String PREF_NAME = "taktak_biometric_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled";
    private static final String KEY_LAST_UNLOCK_TIMESTAMP = "biometric_last_unlock";
    private static final String KEY_LOCK_TIMEOUT_MS = "biometric_lock_timeout_ms";

    // Default timeout: 30 seconds after going to background
    public static final long DEFAULT_TIMEOUT_MS = 30_000L;

    private static volatile BiometricPreferences instance;
    private final SharedPreferences prefs;
    private boolean manuallyLocked = false;

    private BiometricPreferences(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static BiometricPreferences get(@NonNull Context context) {
        if (instance == null) {
            synchronized (BiometricPreferences.class) {
                if (instance == null) {
                    instance = new BiometricPreferences(context);
                }
            }
        }
        return instance;
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
        if (enabled) {
            recordUnlock();
        }
    }

    public long getTimeoutMs() {
        return prefs.getLong(KEY_LOCK_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
    }

    public void setTimeoutMs(long timeoutMs) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT_MS, timeoutMs).apply();
    }

    public void recordUnlock() {
        manuallyLocked = false;
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    public void forceLock() {
        manuallyLocked = true;
    }

    public boolean isAppLocked() {
        if (!isBiometricEnabled()) {
            return false;
        }
        if (manuallyLocked) {
            return true;
        }
        long lastUnlock = prefs.getLong(KEY_LAST_UNLOCK_TIMESTAMP, 0L);
        long elapsed = System.currentTimeMillis() - lastUnlock;
        return elapsed > getTimeoutMs();
    }
}
