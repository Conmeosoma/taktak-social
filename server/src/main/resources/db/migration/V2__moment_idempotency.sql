-- V2: Thêm cột idempotency_key và payload_hash vào bảng moments để đảm bảo tính bất biến khi retry upload
ALTER TABLE moments
    ADD COLUMN idempotency_key UUID,
    ADD COLUMN payload_hash VARCHAR(64);

-- Unique index trên (sender_id, idempotency_key) khi idempotency_key khác null
CREATE UNIQUE INDEX uq_moments_sender_idempotency
    ON moments (sender_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
