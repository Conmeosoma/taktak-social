package com.taktak.app.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Giữ tương thích PendingIntent cũ; chưa có API reaction nên không báo thành công giả. */
public final class WidgetReactionReceiver extends BroadcastReceiver {
    public static final String ACTION_REACT = "com.taktak.app.widget.ACTION_REACT";
    public static final String EXTRA_EMOJI = "extra_emoji";
    public static final String EXTRA_MOMENT_ID = "extra_moment_id";
    @Override public void onReceive(Context context, Intent intent) { }
}
