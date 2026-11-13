CREATE TABLE ai_conversations (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 title VARCHAR(80) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_ai_conversations_owner ON ai_conversations(user_id,updated_at DESC,id DESC);
CREATE TABLE ai_messages (
 id UUID PRIMARY KEY, conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
 client_request_id UUID NOT NULL, role VARCHAR(10) NOT NULL CHECK(role IN ('user','assistant')),
 content TEXT NOT NULL CHECK(char_length(content) BETWEEN 1 AND 16000),
 input_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(conversation_id,client_request_id,role)
);
CREATE INDEX ix_ai_messages_history ON ai_messages(conversation_id,created_at,id);
CREATE TABLE ai_requests (
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, client_request_id UUID NOT NULL,
 conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
 request_hash VARCHAR(64) NOT NULL, message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
 PRIMARY KEY(user_id,client_request_id)
);
