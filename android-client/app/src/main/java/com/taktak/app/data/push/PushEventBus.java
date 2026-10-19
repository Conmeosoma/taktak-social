package com.taktak.app.data.push;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PushEventBus {
    public interface Listener {
        void onMomentCreated(String momentId);
        void onMomentDeleted(String momentId);
        default void onSessionCleared() { }
        default void onChatChanged(String conversationId, String messageId) { }
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

    public static void emitChatChanged(String conversationId, String messageId) {
        if (!com.taktak.app.data.model.FriendModels.uuid(conversationId)
                || !com.taktak.app.data.model.FriendModels.uuid(messageId)) return;
        for (Listener listener : listeners) {
            try { listener.onChatChanged(conversationId, messageId); } catch (Exception ignored) { }
        }
    }

    // Sự kiện local, không phải payload FCM: ẩn dữ liệu đang hiển thị khi mất phiên.
    public static void emitSessionCleared() {
        for (Listener listener : listeners) {
            try { listener.onSessionCleared(); } catch (Exception ignored) { }
        }
    }
}
