package com.taktak.app.data.push;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.repository.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaktakMessagingService extends FirebaseMessagingService {
    private static final String TAG = "TaktakFCM";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM registration token received");
        executor.execute(() -> {
            try {
                AuthRepository auth = AuthGraph.repository(getApplicationContext());
                if (auth.hasSession()) {
                    auth.devices(getApplicationContext()).register(token);
                    Log.d(TAG, "FCM token registered with Taktak server successfully");
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to register new FCM token with server: " + e.getMessage());
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (!remoteMessage.getData().isEmpty()) {
            java.util.Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");
            String momentId = data.get("momentId");
            if ("chat_message".equals(type)) {
                String conversationId = data.get("conversationId"), messageId = data.get("messageId");
                if (!com.taktak.app.data.model.FriendModels.uuid(conversationId)
                        || !com.taktak.app.data.model.FriendModels.uuid(messageId)) return;
                // Push chỉ là tín hiệu: xác minh lại quyền bằng phiên hiện hành trước khi thông báo.
                executor.execute(() -> {
                    try {
                        var repository = AuthGraph.repository(getApplicationContext()).chat();
                        repository.conversation(conversationId);
                        repository.publishIfCurrent(() -> {
                            PushEventBus.emitChatChanged(conversationId, messageId);
                            ChatNotifications.show(getApplicationContext(), conversationId);
                        });
                    } catch (Exception ignored) { Log.d(TAG, "Chat event unavailable for current session"); }
                });
            } else if ("moment_created".equals(type) && momentId != null) {
                PushEventBus.emitMomentCreated(momentId);
                com.taktak.app.widget.WidgetUpdater.enqueueOneTimeWork(getApplicationContext());
            } else if ("moment_deleted".equals(type) && momentId != null) {
                com.taktak.app.ui.history.HistoryImageCache.remove(momentId);
                PushEventBus.emitMomentDeleted(momentId);
                com.taktak.app.widget.WidgetUpdater.enqueueOneTimeWork(getApplicationContext());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
