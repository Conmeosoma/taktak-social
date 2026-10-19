package com.taktak.app.data.repository;

import com.taktak.app.data.model.ChatModels.*;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.Session;
import java.io.IOException;
import java.time.Instant;
import retrofit2.Call;

/** Mỗi repository thuộc một thế hệ phiên; không trả dữ liệu cho tài khoản đã đổi. */
public final class ChatRepository {
    private final ApiClient client;
    private final Session owner;
    public ChatRepository(ApiClient client) { this.client = client; owner = client.sessions.snapshot(); }
    public void requireCurrent() throws ApiFailure {
        Session now = client.sessions.snapshot();
        if (now == null) throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED);
        if (owner == null || owner.generation != now.generation) throw new ApiFailure(ApiFailure.Kind.CANCELLED);
    }
    public boolean publishIfCurrent(Runnable action) {
        return owner != null && client.sessions.runIfCurrent(owner.generation, action);
    }
    public Page<Conversation> inbox(int page) throws IOException {
        checkPage(page); Page<Conversation> result = body(client.chat.inbox(page)); checkPageBody(result);
        for (Conversation item : result.items) checkConversation(item);
        return result;
    }
    public Conversation withFriend(String id) throws IOException {
        checkId(id); Conversation item = body(client.chat.withFriend(id)); checkConversation(item);
        if (!id.equalsIgnoreCase(item.peer.id)) throw invalid();
        return item;
    }
    public Conversation conversation(String id) throws IOException {
        checkId(id); Conversation item = body(client.chat.conversation(id)); checkConversation(item);
        if (!id.equalsIgnoreCase(item.id)) throw invalid();
        return item;
    }
    public Page<Message> messages(String id, int page) throws IOException {
        checkId(id); checkPage(page); Page<Message> result = body(client.chat.messages(id, page)); checkPageBody(result);
        for (Message item : result.items) { checkMessage(item); if (!id.equalsIgnoreCase(item.conversationId)) throw invalid(); }
        return result;
    }
    public Message send(String id, Send request) throws IOException {
        checkId(id);
        if (request == null) throw validation();
        checkId(request.clientMessageId);
        if ("TEXT".equals(request.messageType)) {
            if (!validText(request.content) || request.momentId != null) throw validation();
        } else if ("MOMENT".equals(request.messageType)) {
            checkId(request.momentId);
            if (request.content != null) throw validation();
        } else throw validation();
        Message item = body(client.chat.send(id, request)); checkMessage(item);
        if (!id.equalsIgnoreCase(item.conversationId) || !request.clientMessageId.equalsIgnoreCase(item.clientMessageId)
                || !request.messageType.equals(item.messageType)) throw invalid();
        return item;
    }
    public void read(String id, String messageId) throws IOException {
        checkId(id); checkId(messageId); requireCurrent();
        var response = client.chat.read(id, new Read(messageId)).execute();
        if (!response.isSuccessful()) throw ApiFailure.from(response);
        requireCurrent();
        if (response.code() != 204) throw invalid();
    }
    public static boolean validText(String text) {
        return text != null && !text.strip().isEmpty() && text.codePointCount(0, text.length()) <= 2000
                && text.indexOf('\0') < 0;
    }
    private <T> T body(Call<T> call) throws IOException {
        requireCurrent(); var response = call.execute();
        if (!response.isSuccessful()) throw ApiFailure.from(response);
        requireCurrent(); if (response.body() == null) throw invalid();
        return response.body();
    }
    private static void checkConversation(Conversation item) throws ApiFailure {
        if (item == null || !FriendModels.uuid(item.id) || item.peer == null || !FriendModels.uuid(item.peer.id)
                || item.peer.username == null || item.peer.displayName == null || item.unreadCount < 0
                || !timestamp(item.updatedAt)) throw invalid();
        if (item.lastMessage != null) {
            checkMessage(item.lastMessage);
            if (!item.id.equalsIgnoreCase(item.lastMessage.conversationId)) throw invalid();
        }
    }
    private static void checkMessage(Message item) throws ApiFailure {
        if (item == null || !FriendModels.uuid(item.id) || !FriendModels.uuid(item.conversationId)
                || !FriendModels.uuid(item.senderId) || !FriendModels.uuid(item.recipientId)
                || !FriendModels.uuid(item.clientMessageId) || !timestamp(item.createdAt)
                || (item.readAt != null && !timestamp(item.readAt))) throw invalid();
        if ("TEXT".equals(item.messageType)) { if (!validText(item.content) || item.momentId != null) throw invalid(); }
        else if ("MOMENT".equals(item.messageType)) {
            if (item.momentId != null && !FriendModels.uuid(item.momentId)) throw invalid();
            if (item.content != null && item.content.codePointCount(0, item.content.length()) > 50) throw invalid();
        } else throw invalid();
    }
    private static boolean timestamp(String value) {
        try { Instant.parse(value); return true; } catch (RuntimeException failure) { return false; }
    }
    private static void checkPageBody(Page<?> page) throws ApiFailure {
        if (page == null || page.items == null || page.items.size() > 50) throw invalid();
    }
    private static void checkId(String id) throws ApiFailure { if (!FriendModels.uuid(id)) throw validation(); }
    private static void checkPage(int page) throws ApiFailure { if (page < 0 || page > 10000) throw validation(); }
    private static ApiFailure validation() { return new ApiFailure(ApiFailure.Kind.VALIDATION); }
    private static ApiFailure invalid() { return new ApiFailure(ApiFailure.Kind.SERVER); }
}
