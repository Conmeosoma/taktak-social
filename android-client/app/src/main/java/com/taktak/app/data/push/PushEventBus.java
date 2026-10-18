package com.taktak.app.data.push;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PushEventBus {
    public interface Listener {
        void onMomentCreated(String momentId);
        void onMomentDeleted(String momentId);
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private PushEventBus() {}

    public static void register(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unregister(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static void emitMomentCreated(String momentId) {
        if (momentId != null && !momentId.isBlank()) {
            for (Listener listener : listeners) {
                try {
                    listener.onMomentCreated(momentId);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void emitMomentDeleted(String momentId) {
        if (momentId != null && !momentId.isBlank()) {
            for (Listener listener : listeners) {
                try {
                    listener.onMomentDeleted(momentId);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void clear() {
        listeners.clear();
    }
}
