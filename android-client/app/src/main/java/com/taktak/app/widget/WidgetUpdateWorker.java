package com.taktak.app.widget;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.MomentModels.MomentItem;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.MomentRepository;
import java.io.IOException;

public class WidgetUpdateWorker extends Worker {

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            AuthRepository auth = AuthGraph.repository(context);
            if (!auth.hasSession()) {
                WidgetUpdater.renderLoggedOut(context);
                return Result.success();
            }

            MomentRepository moments = auth.moments();
            MomentItem latest = moments.latest();
            if (latest == null) {
                WidgetUpdater.renderEmpty(context);
                return Result.success();
            }

            byte[] imageBytes = moments.download(latest.id);
            Bitmap bitmap = WidgetUpdater.decodeSampledBitmap(imageBytes, 512, 512);
            WidgetUpdater.renderMoment(context, latest, bitmap);
            return Result.success();
        } catch (ApiFailure failure) {
            if (failure.kind == ApiFailure.Kind.UNAUTHORIZED || failure.kind == ApiFailure.Kind.CANCELLED) {
                WidgetUpdater.renderLoggedOut(context);
                return Result.success();
            }
            if (failure.kind == ApiFailure.Kind.SERVER || failure.kind == ApiFailure.Kind.RATE_LIMIT) {
                return Result.retry();
            }
            return Result.failure();
        } catch (IOException e) {
            return Result.retry();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
