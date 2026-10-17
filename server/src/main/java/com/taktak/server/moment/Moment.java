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

    protected Moment() { }
    public Moment(UUID id, UUID senderId, String caption, String filePath, long byteSize, Instant createdAt) {
        this.id = id; this.senderId = senderId; this.caption = caption; this.filePath = filePath;
        this.mimeType = "image/jpeg"; this.byteSize = byteSize; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public String getFilePath() { return filePath; }
    public Instant getDeletedAt() { return deletedAt; }
}
