package com.taktak.app.ui.history;

import android.graphics.Bitmap;
import androidx.collection.LruCache;

public final class HistoryImageCache {
    private static final int MAX_MEMORY = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int CACHE_SIZE = Math.min(MAX_MEMORY / 8, 20 * 1024); // max 20 MB or 1/8 heap
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private HistoryImageCache() { }

    public static Bitmap get(String key) {
        if (key == null) { return null; }
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    public static void put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) { return; }
        synchronized (CACHE) {
            CACHE.put(key, bitmap);
        }
    }

    public static void remove(String key) {
        if (key == null) { return; }
        synchronized (CACHE) {
            CACHE.remove(key);
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.evictAll();
        }
    }
}
