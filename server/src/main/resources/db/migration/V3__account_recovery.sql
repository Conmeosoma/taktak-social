ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0 CHECK (security_version >= 0);

CREATE TABLE account_recovery (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    verification_hash VARCHAR(64),
    verification_expires_at TIMESTAMPTZ,
    temporary_hash VARCHAR(255),
    temporary_expires_at TIMESTAMPTZ,
    reset_hash VARCHAR(64),
    reset_expires_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_recovery_reset_hash ON account_recovery(reset_hash) WHERE reset_hash IS NOT NULL;
