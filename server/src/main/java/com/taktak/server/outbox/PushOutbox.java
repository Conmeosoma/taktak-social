package com.taktak.server.outbox;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Chỉ lưu định danh; không lưu nội dung riêng tư hoặc FCM token. */
@Service
public class PushOutbox {
    private final JdbcTemplate jdbc;
    public PushOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String type, UUID resource, UUID conversation, UUID sender, UUID recipient) {
        jdbc.update("""
                INSERT INTO push_outbox(id,event_type,resource_id,conversation_id,sender_id,recipient_id)
                VALUES(?,?,?,?,?,?) ON CONFLICT(event_type,resource_id,recipient_id) DO NOTHING
                """, UUID.randomUUID(), type, resource, conversation, sender, recipient);
    }
}
