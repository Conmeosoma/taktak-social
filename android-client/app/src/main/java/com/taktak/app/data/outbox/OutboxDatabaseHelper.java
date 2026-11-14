package com.taktak.app.data.outbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OutboxDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "taktak_outbox.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_OUTBOX = "outbox_tasks";

    public OutboxDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_OUTBOX + " ("
                + "id TEXT PRIMARY KEY, "
                + "owner_user_id TEXT NOT NULL, "
                + "session_generation INTEGER NOT NULL, "
                + "idempotency_key TEXT NOT NULL, "
                + "media_type TEXT NOT NULL, "
                + "payload_path TEXT NOT NULL, "
                + "thumbnail_path TEXT, "
                + "caption TEXT, "
                + "recipient_ids TEXT, "
                + "poll_json TEXT, created_moment_id TEXT, "
                + "created_at INTEGER NOT NULL, "
                + "status TEXT NOT NULL, "
                + "attempt_count INTEGER NOT NULL DEFAULT 0, "
                + "last_attempt_at INTEGER NOT NULL DEFAULT 0, "
                + "last_error TEXT, "
                + "upload_progress INTEGER NOT NULL DEFAULT 0"
                + ")");
        db.execSQL("CREATE INDEX idx_outbox_session ON " + TABLE_OUTBOX + "(session_generation, status)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE " + TABLE_OUTBOX + " ADD COLUMN created_moment_id TEXT");
    }

    public synchronized void insert(OutboxRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = toContentValues(record);
        db.insertWithOnConflict(TABLE_OUTBOX, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized OutboxRecord get(String id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_OUTBOX, null, "id = ?", new String[]{id}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    public synchronized List<OutboxRecord> listActive(long sessionGeneration) {
        SQLiteDatabase db = getReadableDatabase();
        List<OutboxRecord> result = new ArrayList<>();
        String where = "session_generation = ? AND status IN ('PENDING', 'WAITING_NETWORK', 'UPLOADING', 'FAILED_RETRYABLE')";
        try (Cursor c = db.query(TABLE_OUTBOX, null, where, new String[]{String.valueOf(sessionGeneration)}, null, null, "created_at ASC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    result.add(fromCursor(c));
                }
            }
        }
        return result;
    }

    public synchronized List<OutboxRecord> listAllForSession(long sessionGeneration) {
        SQLiteDatabase db = getReadableDatabase();
        List<OutboxRecord> result = new ArrayList<>();
        try (Cursor c = db.query(TABLE_OUTBOX, null, "session_generation = ?", new String[]{String.valueOf(sessionGeneration)}, null, null, "created_at DESC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    result.add(fromCursor(c));
                }
            }
        }
        return result;
    }

    public synchronized boolean updateStatus(String id, OutboxStatus status, String error, int progress, long generation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status.name());
        if (error != null) {
            cv.put("last_error", error);
        }
        cv.put("upload_progress", progress);
        cv.put("last_attempt_at", System.currentTimeMillis());
        return db.update(TABLE_OUTBOX, cv, "id = ? AND session_generation = ? AND status NOT IN ('CANCELLED','COMPLETED')",
                new String[]{id, Long.toString(generation)}) == 1;
    }

    public synchronized void incrementAttempt(String id, OutboxStatus status, String error, long generation) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_OUTBOX + " SET attempt_count = attempt_count + 1, "
                + "status = ?, last_error = ?, last_attempt_at = ? WHERE id = ? AND session_generation = ? AND status NOT IN ('CANCELLED','COMPLETED')",
                new Object[]{status.name(), error, System.currentTimeMillis(), id, generation});
    }

    public synchronized String createdMomentId(String id, long generation) {
        try (Cursor c = getReadableDatabase().query(TABLE_OUTBOX, new String[]{"created_moment_id"},
                "id = ? AND session_generation = ?", new String[]{id, Long.toString(generation)}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }
    public synchronized boolean saveCreatedMomentId(String id, long generation, String momentId) {
        ContentValues values = new ContentValues(); values.put("created_moment_id", momentId);
        return getWritableDatabase().update(TABLE_OUTBOX, values,
                "id = ? AND session_generation = ? AND status NOT IN ('CANCELLED','COMPLETED')",
                new String[]{id, Long.toString(generation)}) == 1;
    }

    public synchronized void delete(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_OUTBOX, "id = ?", new String[]{id});
    }

    public synchronized void cancelAllForSession(long sessionGeneration) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", OutboxStatus.CANCELLED.name());
        db.update(TABLE_OUTBOX, cv, "session_generation = ? AND status NOT IN ('COMPLETED', 'CANCELLED')",
                new String[]{String.valueOf(sessionGeneration)});
    }

    public synchronized void purgeOldCompleted(long maxAgeMillis) {
        SQLiteDatabase db = getWritableDatabase();
        long threshold = System.currentTimeMillis() - maxAgeMillis;
        db.delete(TABLE_OUTBOX, "status IN ('COMPLETED', 'CANCELLED') AND created_at < ?",
                new String[]{String.valueOf(threshold)});
    }

    private static ContentValues toContentValues(OutboxRecord record) {
        ContentValues cv = new ContentValues();
        cv.put("id", record.id);
        cv.put("owner_user_id", record.ownerUserId);
        cv.put("session_generation", record.sessionGeneration);
        cv.put("idempotency_key", record.idempotencyKey);
        cv.put("media_type", record.mediaType);
        cv.put("payload_path", record.payloadPath);
        cv.put("thumbnail_path", record.thumbnailPath);
        cv.put("caption", record.caption);
        cv.put("recipient_ids", serializeRecipients(record.recipientIds));
        cv.put("poll_json", record.pollJson);
        cv.put("created_at", record.createdAt);
        cv.put("status", record.status.name());
        cv.put("attempt_count", record.attemptCount);
        cv.put("last_attempt_at", record.lastAttemptAt);
        cv.put("last_error", record.lastError);
        cv.put("upload_progress", record.uploadProgress);
        return cv;
    }

    private static OutboxRecord fromCursor(Cursor c) {
        String id = c.getString(c.getColumnIndexOrThrow("id"));
        String ownerUserId = c.getString(c.getColumnIndexOrThrow("owner_user_id"));
        long sessionGen = c.getLong(c.getColumnIndexOrThrow("session_generation"));
        String idempotencyKey = c.getString(c.getColumnIndexOrThrow("idempotency_key"));
        String mediaType = c.getString(c.getColumnIndexOrThrow("media_type"));
        String payloadPath = c.getString(c.getColumnIndexOrThrow("payload_path"));
        String thumbnailPath = c.getString(c.getColumnIndexOrThrow("thumbnail_path"));
        String caption = c.getString(c.getColumnIndexOrThrow("caption"));
        List<String> recipients = deserializeRecipients(c.getString(c.getColumnIndexOrThrow("recipient_ids")));
        String pollJson = c.getString(c.getColumnIndexOrThrow("poll_json"));
        long createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        OutboxStatus status;
        try {
            status = OutboxStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status")));
        } catch (Exception e) {
            status = OutboxStatus.PENDING;
        }
        int attemptCount = c.getInt(c.getColumnIndexOrThrow("attempt_count"));
        long lastAttemptAt = c.getLong(c.getColumnIndexOrThrow("last_attempt_at"));
        String lastError = c.getString(c.getColumnIndexOrThrow("last_error"));
        int uploadProgress = c.getInt(c.getColumnIndexOrThrow("upload_progress"));

        return new OutboxRecord(id, ownerUserId, sessionGen, idempotencyKey, mediaType,
                payloadPath, thumbnailPath, caption, recipients, pollJson, createdAt,
                status, attemptCount, lastAttemptAt, lastError, uploadProgress);
    }

    private static String serializeRecipients(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private static List<String> deserializeRecipients(String val) {
        if (val == null || val.trim().isEmpty()) return Collections.emptyList();
        String[] parts = val.split(",");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (!p.trim().isEmpty()) result.add(p.trim());
        }
        return result;
    }
}

