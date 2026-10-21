package com.taktak.app.ui.history;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;

public final class HistoryImageCache {
    private static final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int CACHE_SIZE = Math.min(MAX_MEMORY / 8, 20 * 1024); // max 20 MB or 1/8 heap
    private static final SessionImageCache<Bitmap> CACHE = new SessionImageCache<>(CACHE_SIZE,
            5 * 60 * 1000L, SystemClock::elapsedRealtime, bitmap -> Math.max(1, bitmap.getByteCount() / 1024));

    private HistoryImageCache() { }

    /** Decode cache only, never an authorization grant. Caller must revalidate with server. */
    public static Bitmap getAuthorized(String key, long generation) { return CACHE.get(key, generation); }

    /** Call only inside the validated repository's publishIfCurrent block. */
    public static void rememberAuthorized(String key, long generation, Bitmap bitmap) { CACHE.put(key, generation, bitmap); }

    // Chỉ gọi sau GET ảnh thành công và trong publishIfCurrent; cache không cấp quyền offline.
    public static Bitmap decodeAuthorized(String key, long generation, byte[] bytes) {
        Bitmap cached = CACHE.get(key, generation);
        if (cached != null && !cached.isRecycled()) { return cached; }
        Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (decoded != null) { CACHE.put(key, generation, decoded); }
        return decoded;
    }

    public static void remove(String key) {
        if (key == null) { return; }
        CACHE.remove(key);
    }

    public static void clear() {
        CACHE.clear();
    }
}
