package com.taktak.app.data.repository;

import com.taktak.app.data.model.AiModels.*;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.Session;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import retrofit2.Call;

/** Không tự retry mutation: UI giữ nguyên Send khi kết quả gửi chưa rõ. */
public final class AiRepository {
    private final ApiClient client;
    private final Session owner;
    public AiRepository(ApiClient client) {
        this.client = client;
        owner = client.sessions.snapshot();
    }
    public void requireCurrent() throws ApiFailure {
        Session current = client.sessions.snapshot();
        if (current == null) throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED);
        if (owner == null || current.generation != owner.generation)
            throw new ApiFailure(ApiFailure.Kind.CANCELLED);
    }
    public boolean publishIfCurrent(Runnable action) {
        return owner != null && client.sessions.runIfCurrent(owner.generation, action);
    }
    public Status status() throws IOException {
        Status result = body(client.ai.status());
        if (result.availability == null || !result.availability.matches("[A-Z_]{1,64}")
                || result.privacyNotice == null || result.privacyNotice.isBlank()
                || result.privacyNotice.length() > 4000) throw invalid();
        return result;
    }
    public Page<Conversation> conversations(int page) throws IOException {
        checkPage(page);
        Page<Conversation> result = body(client.ai.conversations(page));
        checkPageBody(result);
        var ids = new HashSet<String>();
        for (Conversation item : result.items) {
            checkConversation(item);
            if (!ids.add(item.id)) throw invalid();
        }
        return result;
    }
    public Conversation create() throws IOException {
        Conversation result = body(client.ai.create()); checkConversation(result); return result;
    }
    public Page<Message> messages(String id, int page) throws IOException {
        checkId(id); checkPage(page);
        Page<Message> result = body(client.ai.messages(id, page));
        checkPageBody(result);
        var ids = new HashSet<String>();
        for (Message item : result.items) {
            checkMessage(item);
            if (!id.equalsIgnoreCase(item.conversationId) || !ids.add(item.id)) throw invalid();
        }
        return result;
    }
    public Message send(Send request) throws IOException {
        if (request == null || !request.shareWithGoogle || !validText(request.message)) throw validation();
        checkId(request.clientRequestId);
        if (request.conversationId != null) checkId(request.conversationId);
        Message result = body(client.ai.send(request)); checkMessage(result);
        if (!"assistant".equals(result.role) || (request.conversationId != null
                && !request.conversationId.equalsIgnoreCase(result.conversationId))) throw invalid();
        return result;
    }
    public void delete(String id) throws IOException {
        checkId(id); requireCurrent();
        var response = client.ai.delete(id).execute();
        requireCurrent();
        if (!response.isSuccessful()) throw ApiFailure.from(response);
        if (response.code() != 204) throw invalid();
    }
    // Khớp @Size(max=4000) Java phía server: tính theo UTF-16, không theo code point.
    public static boolean validText(String value) {
        return value != null && !value.isBlank() && value.length() <= 4000 && value.indexOf('\0') < 0;
    }
    private <T> T body(Call<T> call) throws IOException {
        requireCurrent();
        var response = call.execute();
        requireCurrent();
        if (!response.isSuccessful()) throw ApiFailure.from(response);
        if (response.body() == null) throw invalid();
        return response.body();
    }
    private static void checkConversation(Conversation item) throws ApiFailure {
        if (item == null || !FriendModels.uuid(item.id) || item.title == null || item.title.length() > 200
                || !timestamp(item.createdAt) || !timestamp(item.updatedAt)) throw invalid();
    }
    private static void checkMessage(Message item) throws ApiFailure {
        if (item == null || !FriendModels.uuid(item.id) || !FriendModels.uuid(item.conversationId)
                || !("user".equals(item.role) || "assistant".equals(item.role))
                || item.content == null || item.content.isBlank() || item.content.indexOf('\0') >= 0
                || item.content.length() > ("user".equals(item.role) ? 4000 : 16000)
                || item.inputTokens < 0 || item.outputTokens < 0 || !timestamp(item.createdAt)) throw invalid();
    }
    private static boolean timestamp(String value) {
        try { Instant.parse(value); return true; } catch (RuntimeException error) { return false; }
    }
    private static void checkPageBody(Page<?> page) throws ApiFailure {
        if (page == null || page.items == null || page.items.size() > 20) throw invalid();
    }
    private static void checkPage(int page) throws ApiFailure { if (page < 0 || page > 10000) throw validation(); }
    private static void checkId(String id) throws ApiFailure { if (!FriendModels.uuid(id)) throw validation(); }
    private static ApiFailure validation() { return new ApiFailure(ApiFailure.Kind.VALIDATION); }
    private static ApiFailure invalid() { return new ApiFailure(ApiFailure.Kind.SERVER); }
}
