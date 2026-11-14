package com.taktak.server.moment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moments")
public class Moment {
    @Id private UUID id;
    @Column(name = "sender_id", nullable = false) private UUID senderId;
    @Column(nullable = false, length = 50) private String caption;
    @Column(name = "file_path", nullable = false, unique = true, columnDefinition = "text") private String filePath;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "idempotency_key") private UUID idempotencyKey;
    @Column(name = "payload_hash", length = 64) private String payloadHash;
    @Column(name = "media_type", nullable = false, length = 20) private String mediaType = "PHOTO";
    @Column(name = "video_path") private String videoPath;
    @Column(name = "video_byte_size") private Long videoByteSize;
    @Column(name = "duration_ms") private Integer durationMs;
    @Column(name = "has_audio", nullable = false) private boolean hasAudio = false;

    protected Moment() { }
    public Moment(UUID id, UUID senderId, String caption, String filePath, long byteSize, Instant createdAt) {
        this(id, senderId, caption, filePath, byteSize, createdAt, null, null);
    }
    public Moment(UUID id, UUID senderId, String caption, String filePath, long byteSize, Instant createdAt,
            UUID idempotencyKey, String payloadHash) {
        this(id, senderId, caption, filePath, byteSize, createdAt, idempotencyKey, payloadHash, "PHOTO", null, null, null, false);
    }
    public Moment(UUID id, UUID senderId, String caption, String filePath, long byteSize, Instant createdAt,
            UUID idempotencyKey, String payloadHash, String mediaType, String videoPath, Long videoByteSize,
            Integer durationMs, boolean hasAudio) {
        this.id = id; this.senderId = senderId; this.caption = caption; this.filePath = filePath;
        this.mimeType = "image/jpeg"; this.byteSize = byteSize; this.createdAt = createdAt;
        this.idempotencyKey = idempotencyKey; this.payloadHash = payloadHash;
        this.mediaType = mediaType != null ? mediaType : "PHOTO";
        this.videoPath = videoPath; this.videoByteSize = videoByteSize;
        this.durationMs = durationMs; this.hasAudio = hasAudio;
    }
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public String getFilePath() { return filePath; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public String getCaption() { return caption; }
    public Instant getCreatedAt() { return createdAt; }
    public String getMediaType() { return mediaType; }
    public String getVideoPath() { return videoPath; }
    public Long getVideoByteSize() { return videoByteSize; }
    public Integer getDurationMs() { return durationMs; }
    public boolean isHasAudio() { return hasAudio; }
    public void markDeleted(Instant deletedAt) { this.deletedAt = deletedAt; }
}
