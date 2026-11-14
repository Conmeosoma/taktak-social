package com.taktak.app.data.outbox;

public enum OutboxStatus {
    PENDING,
    WAITING_NETWORK,
    UPLOADING,
    COMPLETED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    CANCELLED
}

