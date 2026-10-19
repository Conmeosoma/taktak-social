package com.taktak.app.data.push;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import com.taktak.app.MainActivity;
import com.taktak.app.R;

/** Chỉ hiển thị thông báo chung, không chứa nội dung tin hoặc ảnh riêng tư. */
public final class ChatNotifications {
    private static final String CHANNEL = "taktak_chat";
    private static volatile int started;
    private static boolean initialized;
    private ChatNotifications() { }
    public static synchronized void initialize(Application app) {
        if (initialized) return;
        initialized = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) { started++; }
            @Override public void onActivityStopped(Activity activity) { started = Math.max(0, started - 1); }
            @Override public void onActivityCreated(Activity activity, Bundle saved) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle saved) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
    public static boolean foreground() { return started > 0; }
    public static void show(Context context, String conversationId) {
        if (foreground() || Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, context.getString(R.string.chat_title), NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(context, MainActivity.class).putExtra("chat.conversation", conversationId)
                .setAction("com.taktak.app.CHAT." + conversationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, conversationId.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify("chat." + conversationId, 1, new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_chat).setContentTitle(context.getString(R.string.chat_title))
                .setContentText(context.getString(R.string.chat_notification)).setContentIntent(pending)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build());
    }
    public static void clear(Context context) {
        var manager = context.getSystemService(NotificationManager.class);
        for (var item : manager.getActiveNotifications()) {
            if (item.getTag() != null && item.getTag().startsWith("chat.")) manager.cancel(item.getTag(), item.getId());
        }
    }
}
