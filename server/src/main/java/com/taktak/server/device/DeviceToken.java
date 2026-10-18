package com.taktak.server.device;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uq_device_tokens_user_device", columnNames = {"user_id", "device_id"})
})
public class DeviceToken {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fcm_token", nullable = false, unique = true, columnDefinition = "text")
    private String fcmToken;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() { }

    public DeviceToken(UUID id, UUID userId, String deviceId, String fcmToken, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getFcmToken() { return fcmToken; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
