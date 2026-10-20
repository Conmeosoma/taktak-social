package com.taktak.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class TaktakAppWidgetProvider4x2 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetUpdater.enqueueOneTimeWork(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        WidgetUpdater.enqueuePeriodicWork(context);
        WidgetUpdater.enqueueOneTimeWork(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        WidgetUpdater.cancelPeriodicWork(context);
    }
}
