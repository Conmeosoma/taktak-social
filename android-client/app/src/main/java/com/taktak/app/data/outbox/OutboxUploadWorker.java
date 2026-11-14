package com.taktak.app.data.outbox;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.MomentRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OutboxUploadWorker extends Worker {
    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_SESSION_GEN = "session_gen";

    public OutboxUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String taskId = getInputData().getString(KEY_TASK_ID);
        long expectedSessionGen = getInputData().getLong(KEY_SESSION_GEN, -1);

        if (taskId == null || taskId.isEmpty()) {
            return Result.failure();
        }

        OutboxRepository outbox = OutboxRepository.getInstance(context);
        OutboxRecord task = outbox.get(taskId);
        if (task == null) {
            return Result.failure();
        }
        if (task.sessionGeneration != expectedSessionGen) return Result.failure();

        if (task.status == OutboxStatus.CANCELLED || task.status == OutboxStatus.COMPLETED) {
            return Result.success();
        }

        AuthRepository auth;
        try {
            auth = AuthGraph.repository(context);
            if (!auth.hasSession() || auth.sessionGeneration() != expectedSessionGen
                    || !task.ownerUserId.equals(auth.profile().id)) {
                // Session expired, logged out, or changed -> cancel task safely
                outbox.cancel(taskId, expectedSessionGen);
                return Result.failure();
            }
        } catch (Exception e) {
            return Result.retry();
        }

        String savedMomentId = outbox.createdMomentId(taskId, expectedSessionGen);
        File payload = new File(task.payloadPath);
        if (savedMomentId == null && (!payload.exists() || payload.length() < 3)) {
            outbox.updateStatus(taskId, OutboxStatus.FAILED_PERMANENT, "Payload file missing or corrupted", 0, expectedSessionGen);
            return Result.failure();
        }

        if (!outbox.updateStatus(taskId, OutboxStatus.UPLOADING, null, 0, expectedSessionGen)
                || isStopped()) return Result.failure();

        try {
            String createdId = savedMomentId;
            if (createdId == null) {
                byte[] bytes = new byte[(int) payload.length()];
                try (FileInputStream fis = new FileInputStream(payload)) {
                    int read = 0;
                    while (read < bytes.length) {
                        int r = fis.read(bytes, read, bytes.length - read);
                        if (r == -1) break;
                        read += r;
                    }
                }

                MomentRepository moments = auth.moments();
                if ("LIVE_VIDEO".equalsIgnoreCase(task.mediaType) && task.thumbnailPath != null && new File(task.thumbnailPath).exists()) {
                    var metadata = com.taktak.app.data.repository.LiveVideoMetadata.read(payload);
                    File thumb = new File(task.thumbnailPath);
                    byte[] thumbBytes = new byte[(int) thumb.length()];
                    try (FileInputStream tfis = new FileInputStream(thumb)) {
                        int tr = 0;
                        while (tr < thumbBytes.length) {
                            int r = tfis.read(thumbBytes, tr, thumbBytes.length - tr);
                            if (r == -1) break;
                            tr += r;
                        }
                    }
                    createdId = moments.uploadLive(
                            thumbBytes,
                            bytes,
                            task.caption,
                            task.recipientIds,
                            task.idempotencyKey,
                            metadata.durationMs(),
                            metadata.hasAudio(),
                            (percent, bytesWritten, totalBytes) -> {
                                outbox.updateStatus(taskId, OutboxStatus.UPLOADING, null, percent, expectedSessionGen);
                            }
                    );
                } else {
                    createdId = moments.upload(
                            bytes,
                            task.caption,
                            task.recipientIds,
                            task.idempotencyKey,
                            (percent, bytesWritten, totalBytes) -> {
                                outbox.updateStatus(taskId, OutboxStatus.UPLOADING, null, percent, expectedSessionGen);
                            }
                    );
                }
                if (!outbox.saveCreatedMomentId(taskId, expectedSessionGen, createdId)) return Result.failure();
            }

            if (task.pollJson != null && !task.pollJson.isBlank() && createdId != null) {
                if (isStopped() || auth.sessionGeneration() != expectedSessionGen
                        || outbox.get(taskId).status == OutboxStatus.CANCELLED) return Result.failure();
                com.taktak.app.data.model.PollModels.CreatePollRequest pr =
                        new com.google.gson.Gson().fromJson(task.pollJson, com.taktak.app.data.model.PollModels.CreatePollRequest.class);
                if (pr == null || pr.question == null || pr.optionA == null || pr.optionB == null)
                    throw new ApiFailure(ApiFailure.Kind.VALIDATION);
                auth.polls().createPoll(createdId, pr.question, pr.optionA, pr.optionB);
            }

            // Upload succeeded
            outbox.markCompleted(taskId, expectedSessionGen);
            return Result.success();
        } catch (ApiFailure failure) {
            boolean nonRetryable = failure.status == 400 || failure.status == 401 || failure.status == 409
                    || failure.status == 403 || failure.status == 404 || failure.status == 413 || failure.status == 415
                    || failure.kind == ApiFailure.Kind.VALIDATION || failure.kind == ApiFailure.Kind.UNAUTHORIZED;
            if (nonRetryable) {
                outbox.updateStatus(taskId, OutboxStatus.FAILED_PERMANENT, failure.getMessage(), 0, expectedSessionGen);
                return Result.failure();
            } else {
                outbox.incrementAttempt(taskId, OutboxStatus.FAILED_RETRYABLE, failure.getMessage(), expectedSessionGen);
                return Result.retry();
            }
        } catch (IOException e) {
            outbox.incrementAttempt(taskId, OutboxStatus.FAILED_RETRYABLE, e.getMessage(), expectedSessionGen);
            return Result.retry();
        } catch (Exception e) {
            outbox.updateStatus(taskId, OutboxStatus.FAILED_PERMANENT, e.getMessage(), 0, expectedSessionGen);
            return Result.failure();
        }
    }
}
