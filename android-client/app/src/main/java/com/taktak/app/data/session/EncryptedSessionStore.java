package com.taktak.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public final class EncryptedSessionStore implements SessionStore {
    public static final String FILE_NAME = "auth_session_v1";
    private final SharedPreferences preferences;
    private final String origin;

    public EncryptedSessionStore(Context context, String origin) throws IOException {
        this(context, origin, FILE_NAME);
    }

    // Test cùng package dùng file riêng, không xóa phiên người dùng.
    EncryptedSessionStore(Context context, String origin, String fileName) throws IOException {
        this.origin = origin;
        try {
            MasterKey key = new MasterKey.Builder(context.getApplicationContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            preferences = EncryptedSharedPreferences.create(context.getApplicationContext(), fileName, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new IOException("Secure session storage unavailable.");
        }
    }

    @Override public synchronized Session load() throws IOException {
        try {
            String access = preferences.getString("access", null);
            String refresh = preferences.getString("refresh", null);
            if (access == null && refresh == null) { return null; }
            if (!origin.equals(preferences.getString("origin", null)) || access == null || refresh == null
                    || access.isEmpty() || refresh.isEmpty()) { clear(); return null; }
            return new Session(access, refresh, preferences.getLong("expires", 0), 0);
        } catch (RuntimeException exception) { throw new IOException("Secure session storage unreadable."); }
    }

    @Override public synchronized void save(Session session) throws IOException {
        try {
            if (!preferences.edit().putString("origin", origin).putString("access", session.accessToken)
                    .putString("refresh", session.refreshToken).putLong("expires", session.expiresAtMillis).commit()) {
                throw new IOException("Secure session storage write failed.");
            }
        } catch (RuntimeException exception) { throw new IOException("Secure session storage write failed."); }
    }

    @Override public synchronized void clear() throws IOException {
        try {
            if (!preferences.edit().clear().commit()) { throw new IOException("Secure session storage clear failed."); }
        } catch (RuntimeException exception) { throw new IOException("Secure session storage clear failed."); }
    }
}
