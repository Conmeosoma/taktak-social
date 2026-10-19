package com.taktak.server.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Chỉ truy cập khi đã giữ khóa users, cùng thứ tự với login/refresh/reset. */
@Entity
@Table(name = "account_recovery")
public class RecoveryCredentials {
    @Id @Column(name = "user_id") UUID userId;
    @Column(name = "verification_hash", length = 64) String verificationHash;
    @Column(name = "verification_expires_at") Instant verificationExpiresAt;
    @Column(name = "temporary_hash", length = 255) String temporaryHash;
    @Column(name = "temporary_expires_at") Instant temporaryExpiresAt;
    @Column(name = "reset_hash", length = 64) String resetHash;
    @Column(name = "reset_expires_at") Instant resetExpiresAt;
    protected RecoveryCredentials() { }
    RecoveryCredentials(UUID userId) { this.userId = userId; }
    void clearRecovery() {
        temporaryHash = null; temporaryExpiresAt = null; resetHash = null; resetExpiresAt = null;
    }
}
