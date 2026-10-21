CREATE TABLE push_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('moment_created','moment_deleted','chat_message')),
    resource_id UUID NOT NULL,
    conversation_id UUID,
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSING','ACCEPTED','CANCELLED','DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    lease_id UUID,
    lease_until TIMESTAMPTZ,
    last_error VARCHAR(40),
    UNIQUE(event_type,resource_id,recipient_id)
);
CREATE INDEX idx_push_outbox_due ON push_outbox(available_at,created_at) WHERE status IN ('PENDING','PROCESSING');
