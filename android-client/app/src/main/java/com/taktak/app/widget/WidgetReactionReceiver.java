package com.taktak.app.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.taktak.app.R;

public class WidgetReactionReceiver extends BroadcastReceiver {
    public static final String ACTION_REACT = "com.taktak.app.widget.ACTION_REACT";
    public static final String EXTRA_EMOJI = "extra_emoji";
    public static final String EXTRA_MOMENT_ID = "extra_moment_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_REACT.equals(intent.getAction())) {
            return;
        }

        String emoji = intent.getStringExtra(EXTRA_EMOJI);
        String momentId = intent.getStringExtra(EXTRA_MOMENT_ID);

        if (emoji == null) {
            emoji = "❤️";
        }

        // Show immediate visual confirmation
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TaktakAppWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids != null && ids.length > 0) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_taktak_2x2);
            views.setViewVisibility(R.id.widget_reaction_burst, View.VISIBLE);
            views.setTextViewText(R.id.widget_reaction_burst, emoji);
            manager.partiallyUpdateAppWidget(ids, views);
        }

        // Feedback toast
        Toast.makeText(context, context.getString(R.string.widget_reacted, emoji), Toast.LENGTH_SHORT).show();
    }
}
