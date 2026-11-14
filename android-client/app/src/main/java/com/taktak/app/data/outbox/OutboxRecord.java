package com.taktak.app.data.outbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OutboxRecord {
    public final String id;
    public final String ownerUserId;
    public final long sessionGeneration;
    public final String idempotencyKey;
    public final String mediaType; // "PHOTO" or "LIVE_VIDEO"
    public final String payloadPath;
    public final String thumbnailPath;
    public final String caption;
    public final List<String> recipientIds;
    public final String pollJson;
    public final long createdAt;
    public final OutboxStatus status;
    public final int attemptCount;
    public final long lastAttemptAt;
    public final String lastError;
    public final int uploadProgress;

    public OutboxRecord(String id, String ownerUserId, long sessionGeneration, String idempotencyKey,
            String mediaType, String payloadPath, String thumbnailPath, String caption,
            List<String> recipientIds, String pollJson, long createdAt, OutboxStatus status,
            int attemptCount, long lastAttemptAt, String lastError, int uploadProgress) {
        this.id = id;
        this.ownerUserId = ownerUserId == null ? "" : ownerUserId;
        this.sessionGeneration = sessionGeneration;
        this.idempotencyKey = idempotencyKey;
        this.mediaType = mediaType == null ? "PHOTO" : mediaType;
        this.payloadPath = payloadPath;
        this.thumbnailPath = thumbnailPath;
        this.caption = caption == null ? "" : caption;
        this.recipientIds = recipientIds == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(recipientIds));
        this.pollJson = pollJson;
        this.createdAt = createdAt;
        this.status = status == null ? OutboxStatus.PENDING : status;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.lastError = lastError;
        this.uploadProgress = uploadProgress;
    }

    public OutboxRecord withStatus(OutboxStatus newStatus, int progress, String error) {
        return new OutboxRecord(id, ownerUserId, sessionGeneration, idempotencyKey, mediaType,
                payloadPath, thumbnailPath, caption, recipientIds, pollJson, createdAt,
                newStatus, attemptCount, System.currentTimeMillis(), error != null ? error : lastError, progress);
    }

    public OutboxRecord withAttemptIncremented(OutboxStatus newStatus, String error) {
        return new OutboxRecord(id, ownerUserId, sessionGeneration, idempotencyKey, mediaType,
                payloadPath, thumbnailPath, caption, recipientIds, pollJson, createdAt,
                newStatus, attemptCount + 1, System.currentTimeMillis(), error, uploadProgress);
    }
}

