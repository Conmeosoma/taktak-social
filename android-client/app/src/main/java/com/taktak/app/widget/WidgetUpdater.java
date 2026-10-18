package com.taktak.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.data.model.MomentModels.MomentItem;
import java.util.concurrent.TimeUnit;

public final class WidgetUpdater {
    public static final String WORK_NAME_SYNC = "taktak_widget_sync";
    public static final String WORK_NAME_PERIODIC = "taktak_widget_periodic";

    private WidgetUpdater() { }
    public static void clearForLogout(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME_SYNC);
        renderLoggedOut(context);
    }

    public static void enqueueOneTimeWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(WORK_NAME_SYNC, ExistingWorkPolicy.REPLACE, request);
    }

    public static void enqueuePeriodicWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WidgetUpdateWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancelPeriodicWork(Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(WORK_NAME_PERIODIC);
    }

    public static Bitmap decodeSampledBitmap(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) { return null; }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        int inSampleSize = 1;
        while ((options.outHeight / inSampleSize) > reqHeight || (options.outWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void renderMoment(Context context, MomentItem moment, Bitmap bitmap) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TaktakAppWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) { return; }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_taktak_2x2);
        views.setOnClickPendingIntent(R.id.widget_root, buildPendingIntent(context));

        views.setViewVisibility(R.id.widget_moment_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_empty_container, View.GONE);
        views.setViewVisibility(R.id.widget_logged_out_container, View.GONE);

        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_image, bitmap);
        }
        String displayName = (moment.sender != null && moment.sender.displayName != null && !moment.sender.displayName.trim().isEmpty())
                ? moment.sender.displayName
                : (moment.sender != null ? moment.sender.username : "");
        views.setTextViewText(R.id.widget_sender_name, displayName);

        if (moment.caption != null && !moment.caption.trim().isEmpty()) {
            views.setViewVisibility(R.id.widget_caption, View.VISIBLE);
            views.setTextViewText(R.id.widget_caption, moment.caption);
        } else {
            views.setViewVisibility(R.id.widget_caption, View.GONE);
        }
        manager.updateAppWidget(component, views);
    }

    public static void renderEmpty(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TaktakAppWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) { return; }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_taktak_2x2);
        views.setOnClickPendingIntent(R.id.widget_root, buildPendingIntent(context));

        views.setViewVisibility(R.id.widget_moment_container, View.GONE);
        views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_logged_out_container, View.GONE);

        manager.updateAppWidget(component, views);
    }

    public static void renderLoggedOut(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TaktakAppWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) { return; }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_taktak_2x2);
        views.setOnClickPendingIntent(R.id.widget_root, buildPendingIntent(context));

        views.setViewVisibility(R.id.widget_moment_container, View.GONE);
        views.setViewVisibility(R.id.widget_empty_container, View.GONE);
        views.setViewVisibility(R.id.widget_logged_out_container, View.VISIBLE);

        manager.updateAppWidget(component, views);
    }
}
