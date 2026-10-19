package com.taktak.app.data.model;

import java.util.List;

public final class ChatModels {
    private ChatModels() { }
    public static final class Peer {
        public String id, username, displayName, avatarVersion;
    }
    public static final class Conversation {
        public String id, updatedAt;
        public Peer peer;
        public Message lastMessage;
        public long unreadCount;
    }
    public static final class Message {
        public String id, conversationId, senderId, recipientId, clientMessageId;
        public String messageType, content, momentId, createdAt, readAt;
    }
    public static final class Page<T> {
        public List<T> items;
        public boolean hasNext;
        public long totalUnreadCount;
    }
    public static final class Send {
        public final String clientMessageId, messageType, content, momentId;
        public Send(String key, String type, String text, String moment) {
            clientMessageId = key; messageType = type; content = text; momentId = moment;
        }
    }
    public static final class Read {
        public final String messageId;
        public Read(String id) { messageId = id; }
    }
}
