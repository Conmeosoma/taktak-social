package com.taktak.app.data.model;

import java.util.List;

/** DTO riêng cho AI; không dùng model chat bạn bè hoặc media. */
public final class AiModels {
    private AiModels() { }
    public static final class Status {
        public String availability, privacyNotice;
        public boolean canSend() { return "CONFIGURED".equals(availability); }
    }
    public static final class Conversation {
        public String id, title, createdAt, updatedAt;
    }
    public static final class Message {
        public String id, conversationId, role, content, createdAt;
        public int inputTokens, outputTokens;
        @Override public String toString() { return "AiMessage[redacted]"; }
    }
    public static final class Page<T> {
        public List<T> items;
        public boolean hasNext;
    }
    public static final class Send {
        public final String conversationId, clientRequestId, message;
        public final boolean shareWithGoogle;
        public Send(String conversationId, String clientRequestId, String message, boolean shareWithGoogle) {
            this.conversationId = conversationId;
            this.clientRequestId = clientRequestId;
            this.message = message;
            this.shareWithGoogle = shareWithGoogle;
        }
        @Override public String toString() { return "AiSend[redacted]"; }
    }
}
