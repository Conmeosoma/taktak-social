package com.taktak.app.data.outbox;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class OutboxRepository {
    private static volatile OutboxRepository instance;
    private final Context context;
    private final OutboxDatabaseHelper db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<OutboxRecord>> activeTasksLive = new MutableLiveData<>(Collections.emptyList());

    public static OutboxRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (OutboxRepository.class) {
                if (instance == null) {
                    instance = new OutboxRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private OutboxRepository(Context context) {
        this.context = context;
        this.db = new OutboxDatabaseHelper(context);
        // Purge old completed/cancelled records older than 48 hours
        executor.execute(() -> {
            db.purgeOldCompleted(48 * 60 * 60 * 1000L);
        });
    }

    public LiveData<List<OutboxRecord>> getActiveTasks() {
        return activeTasksLive;
    }

    public void refreshActiveTasks(long sessionGeneration) {
        executor.execute(() -> {
            List<OutboxRecord> active = db.listActive(sessionGeneration);
            activeTasksLive.postValue(active);
        });
    }

    public File getOutboxDirectory(long sessionGeneration) {
        File dir = new File(new File(context.getFilesDir(), "outbox"), String.valueOf(sessionGeneration));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public OutboxRecord enqueue(long sessionGeneration, String ownerUserId, byte[] mediaBytes,
            String mediaType, String thumbnailPath, String caption, List<String> recipientIds,
            String pollJson) throws IOException {
        String taskId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        File outboxDir = getOutboxDirectory(sessionGeneration);
        String extension = "LIVE_VIDEO".equals(mediaType) ? ".mp4" : ".jpg";
        File payloadFile = new File(outboxDir, taskId + extension);

        try (FileOutputStream fos = new FileOutputStream(payloadFile)) {
            fos.write(mediaBytes);
            fos.flush();
        }

        OutboxRecord record = new OutboxRecord(
                taskId,
                ownerUserId,
                sessionGeneration,
                idempotencyKey,
                mediaType,
                payloadFile.getAbsolutePath(),
                thumbnailPath,
                caption,
                recipientIds,
                pollJson,
                System.currentTimeMillis(),
                OutboxStatus.PENDING,
                0,
                0,
                null,
                0
        );

        db.insert(record);
        refreshActiveTasks(sessionGeneration);
        scheduleWorker(record);
        return record;
    }

    public void scheduleWorker(OutboxRecord record) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putString(OutboxUploadWorker.KEY_TASK_ID, record.id)
                .putLong(OutboxUploadWorker.KEY_SESSION_GEN, record.sessionGeneration)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(OutboxUploadWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag("outbox_task")
                .addTag("task_" + record.id)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "outbox_" + record.id,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    public void cancel(String taskId, long sessionGeneration) {
        executor.execute(() -> {
            OutboxRecord record = db.get(taskId);
            if (record != null && record.sessionGeneration == sessionGeneration
                    && db.updateStatus(taskId, OutboxStatus.CANCELLED, "Canceled by user", 0, sessionGeneration)) {
                WorkManager.getInstance(context).cancelUniqueWork("outbox_" + taskId);
                if (record.payloadPath != null) {
                    try {
                        Files.deleteIfExists(new File(record.payloadPath).toPath());
                    } catch (Exception ignored) { }
                }
                if (record.thumbnailPath != null) {
                    try {
                        Files.deleteIfExists(new File(record.thumbnailPath).toPath());
                    } catch (Exception ignored) { }
                }
            }
            refreshActiveTasks(sessionGeneration);
        });
    }

    public void retry(String taskId, long sessionGeneration) {
        executor.execute(() -> {
            OutboxRecord record = db.get(taskId);
            if (record != null && record.sessionGeneration == sessionGeneration
                    && (record.status == OutboxStatus.FAILED_RETRYABLE || record.status == OutboxStatus.FAILED_PERMANENT)
                    && db.updateStatus(taskId, OutboxStatus.PENDING, null, 0, sessionGeneration)) {
                OutboxRecord updated = db.get(taskId);
                if (updated != null) {
                    scheduleWorker(updated);
                }
            }
            refreshActiveTasks(sessionGeneration);
        });
    }

    public void handleLogout(long sessionGeneration) {
        // Cancel all WorkManager workers
        WorkManager.getInstance(context).cancelAllWorkByTag("outbox_task");
        executor.execute(() -> {
            db.cancelAllForSession(sessionGeneration);
            File outboxDir = new File(new File(context.getFilesDir(), "outbox"), String.valueOf(sessionGeneration));
            if (outboxDir.exists() && outboxDir.isDirectory()) {
                File[] files = outboxDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
                outboxDir.delete();
            }
            activeTasksLive.postValue(Collections.emptyList());
        });
    }

    public OutboxRecord get(String taskId) {
        return db.get(taskId);
    }

    public boolean updateStatus(String taskId, OutboxStatus status, String error, int progress, long sessionGeneration) {
        boolean changed = db.updateStatus(taskId, status, error, progress, sessionGeneration);
        refreshActiveTasks(sessionGeneration);
        return changed;
    }

    public void incrementAttempt(String taskId, OutboxStatus status, String error, long sessionGeneration) {
        db.incrementAttempt(taskId, status, error, sessionGeneration);
        refreshActiveTasks(sessionGeneration);
    }

    public void markCompleted(String taskId, long sessionGeneration) {
        OutboxRecord record = db.get(taskId);
        boolean completed = db.updateStatus(taskId, OutboxStatus.COMPLETED, null, 100, sessionGeneration);
        if (completed && record != null && record.payloadPath != null) {
            try {
                Files.deleteIfExists(new File(record.payloadPath).toPath());
            } catch (Exception ignored) { }
        }
        refreshActiveTasks(sessionGeneration);
    }
    public String createdMomentId(String taskId, long generation) { return db.createdMomentId(taskId, generation); }
    public boolean saveCreatedMomentId(String taskId, long generation, String momentId) {
        return db.saveCreatedMomentId(taskId, generation, momentId);
    }
}

