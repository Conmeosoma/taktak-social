-- Reuse private chat content/reference; never grant moment access through a message.
ALTER TABLE messages DROP CONSTRAINT messages_message_type_check;
ALTER TABLE messages ADD CONSTRAINT messages_message_type_check
    CHECK (message_type IN ('TEXT', 'MOMENT', 'MOMENT_REPLY'));
ALTER TABLE messages DROP CONSTRAINT ck_messages_content;
ALTER TABLE messages ADD CONSTRAINT ck_messages_content CHECK (
    (message_type = 'TEXT' AND content IS NOT NULL
        AND char_length(content) BETWEEN 1 AND 2000 AND moment_id IS NULL)
    OR message_type = 'MOMENT'
    OR (message_type = 'MOMENT_REPLY' AND content IS NOT NULL
        AND char_length(content) BETWEEN 1 AND 2000)
);
-- moment_id may become NULL through the existing ON DELETE SET NULL foreign key.
