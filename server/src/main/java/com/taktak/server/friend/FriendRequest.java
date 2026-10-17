package com.taktak.server.friend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_requests")
public class FriendRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;
    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "responded_at")
    private Instant respondedAt;

    protected FriendRequest() { }
    public FriendRequest(UUID senderId, UUID receiverId, Instant now) {
        this.senderId = senderId; this.receiverId = receiverId;
        this.status = "pending"; this.createdAt = now;
    }
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getReceiverId() { return receiverId; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void respond(String status, Instant now) {
        this.status = status;
        this.respondedAt = now;
    }
}
