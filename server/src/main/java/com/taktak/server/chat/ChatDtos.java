package com.taktak.server.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {
    private ChatDtos() { }
    public record Peer(UUID id, String username, String displayName, String avatarVersion) { }
    public record Message(UUID id, UUID conversationId, UUID senderId, UUID recipientId,
            UUID clientMessageId, String messageType, String content, UUID momentId, Instant createdAt, Instant readAt) {
        @Override public String toString() { return "Message[redacted]"; }
    }
    public record Conversation(UUID id, Peer peer, Message lastMessage, long unreadCount, Instant updatedAt) { }
    public record ConversationsPage(List<Conversation> items, int page, boolean hasNext, long totalUnreadCount) { }
    public record MessagesPage(List<Message> items, int page, boolean hasNext) { }
    public record SendRequest(@NotNull UUID clientMessageId, @NotNull String messageType,
            @Size(max=4000) String content, UUID momentId) {
        @Override public String toString() { return "SendRequest[redacted]"; }
    }
    public record ReadRequest(@NotNull UUID messageId) { }
}
