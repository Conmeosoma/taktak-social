CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user1_id UUID NOT NULL REFERENCES users(id),
    user2_id UUID NOT NULL REFERENCES users(id),
    last_message_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_conversations_pair CHECK (user1_id < user2_id),
    CONSTRAINT uq_conversations_pair UNIQUE (user1_id, user2_id)
);
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    client_message_id UUID NOT NULL,
    message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('TEXT','MOMENT')),
    content TEXT,
    moment_id UUID REFERENCES moments(id) ON DELETE SET NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_messages_sender_client UNIQUE(sender_id,client_message_id),
    CONSTRAINT ck_messages_participants CHECK(sender_id <> recipient_id),
    CONSTRAINT ck_messages_content CHECK (
        (message_type='TEXT' AND content IS NOT NULL AND char_length(content) BETWEEN 1 AND 2000 AND moment_id IS NULL)
        OR message_type='MOMENT')
);
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_last_message FOREIGN KEY(last_message_id) REFERENCES messages(id);
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id,created_at DESC,id DESC);
CREATE INDEX idx_messages_unread ON messages(conversation_id,recipient_id) WHERE read_at IS NULL;
CREATE INDEX idx_conversations_first ON conversations(user1_id,updated_at DESC,id DESC);
CREATE INDEX idx_conversations_second ON conversations(user2_id,updated_at DESC,id DESC);
