package com.taktak.app.data.session;

/** Credential giới hạn, không đi vào encrypted full-session store/Intent/Bundle. */
public final class ResetSessionVault {
    private static String token;
    private static long expires;
    private ResetSessionVault() { }
    public static synchronized void put(String value, long seconds) {
        clear();
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}") || seconds < 1 || seconds > 900) {
            throw new IllegalArgumentException("Invalid reset session.");
        }
        token=value; expires=android.os.SystemClock.elapsedRealtime()+seconds*1000;
    }
    public static synchronized String peek() {
        if (android.os.SystemClock.elapsedRealtime() >= expires) clear();
        return token;
    }
    public static synchronized void clear() { token=null; expires=0; }
}
