CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    avatar_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_username CHECK (username ~ '^[a-z0-9_]+$'),
    CONSTRAINT ck_users_email_nonempty CHECK (length(trim(email)) > 0)
);
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

CREATE TABLE friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    CONSTRAINT ck_friend_requests_not_self CHECK (sender_id <> receiver_id),
    CONSTRAINT ck_friend_requests_status CHECK (status IN ('pending', 'accepted', 'rejected'))
);
CREATE UNIQUE INDEX uq_friend_requests_pending_pair
    ON friend_requests (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
    WHERE status = 'pending';
CREATE INDEX ix_friend_requests_receiver_status ON friend_requests (receiver_id, status, created_at DESC);

CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_low_id UUID NOT NULL REFERENCES users(id),
    user_high_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_friendships_order CHECK (user_low_id < user_high_id),
    CONSTRAINT uq_friendships_pair UNIQUE (user_low_id, user_high_id)
);
CREATE INDEX ix_friendships_high_user ON friendships (user_high_id);

CREATE TABLE blocks (
    blocker_id UUID NOT NULL REFERENCES users(id),
    blocked_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT ck_blocks_not_self CHECK (blocker_id <> blocked_id)
);
CREATE INDEX ix_blocks_blocked_user ON blocks (blocked_id);

CREATE TABLE moments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    caption VARCHAR(50) NOT NULL DEFAULT '',
    file_path TEXT NOT NULL UNIQUE,
    mime_type VARCHAR(100) NOT NULL,
    byte_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_moments_file_path CHECK (length(trim(file_path)) > 0),
    CONSTRAINT ck_moments_byte_size CHECK (byte_size > 0)
);
CREATE INDEX ix_moments_sender_created ON moments (sender_id, created_at DESC);

CREATE TABLE moment_recipients (
    moment_id UUID NOT NULL REFERENCES moments(id),
    user_id UUID NOT NULL REFERENCES users(id),
    -- Service ghi cùng timestamp với moment trong một transaction.
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (moment_id, user_id)
);
CREATE INDEX ix_moment_recipients_user_created ON moment_recipients (user_id, created_at DESC);

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    fcm_token TEXT NOT NULL UNIQUE,
    device_id VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_device_tokens_user_device UNIQUE (user_id, device_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_refresh_tokens_user_expiry ON refresh_tokens (user_id, expires_at);
