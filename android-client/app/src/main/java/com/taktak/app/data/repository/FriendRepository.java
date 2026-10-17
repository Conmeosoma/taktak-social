package com.taktak.app.data.repository;

import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.model.FriendModels.*;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.session.Session;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;

/** Mỗi instance chỉ phục vụ thế hệ phiên đã tạo nó. */
public final class FriendRepository {
    private final ApiClient client;
    private final Session owner;
    public FriendRepository(ApiClient client) { this.client = client; owner = client.sessions.snapshot(); }
    public void requireCurrent() throws ApiFailure {
        Session now = client.sessions.snapshot();
        if (now == null) { throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED); }
        if (owner == null || now.generation != owner.generation) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
    }
    public static String normalizeQuery(String value) {
        if (value == null || value.length() > 255) { return null; }
        int start = 0, end = value.length();
        while (start < end && Character.isWhitespace(value.codePointAt(start))) { start += Character.charCount(value.codePointAt(start)); }
        while (end > start && Character.isWhitespace(value.codePointBefore(end))) { end -= Character.charCount(value.codePointBefore(end)); }
        String query = value.substring(start, end).toLowerCase(Locale.ROOT);
        return query.matches("[a-z0-9_]{1,255}") ? query : null;
    }
    public List<User> search(String query) throws IOException {
        String normalized = normalizeQuery(query);
        if (normalized == null) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
        List<User> users = body(client.friends.search(normalized));
        checkUsers(users);
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(users));
    }
    public Page<User> friends(int page) throws IOException {
        checkPage(page);
        Page<User> result = body(client.friends.friends(page));
        if (result.page != page) { throw invalid(); }
        checkUsers(result.items);
        return result;
    }
    public Page<Invitation> incoming(int page) throws IOException {
        checkPage(page);
        Page<Invitation> result = body(client.friends.incoming(page));
        if (result.page != page || result.items == null || result.items.size() > 20) { throw invalid(); }
        for (Invitation item : result.items) {
            if (item == null || !item.valid() || !"pending".equals(item.status)) { throw invalid(); }
        }
        return result;
    }
    public Invitation send(String id) throws IOException {
        checkId(id);
        return invitation(client.friends.send(new Send(id)), "pending");
    }
    public Invitation respond(String id, boolean accept) throws IOException {
        checkId(id);
        return invitation(client.friends.respond(id, new Respond(accept)), accept ? "accepted" : "rejected");
    }
    public void unfriend(String id) throws IOException { checkId(id); empty(client.friends.unfriend(id)); }
    public void block(String id) throws IOException { checkId(id); empty(client.friends.block(id)); }
    private Invitation invitation(Call<Invitation> call, String status) throws IOException {
        Invitation item = body(call);
        if (!item.valid() || !status.equals(item.status)) { throw invalid(); }
        return item;
    }
    private <T> T body(Call<T> call) throws IOException {
        requireCurrent();
        var response = call.execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        requireCurrent();
        if (response.body() == null) { throw invalid(); }
        return response.body();
    }
    private void empty(Call<Void> call) throws IOException {
        requireCurrent();
        var response = call.execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        requireCurrent();
        if (response.code() != 204) { throw invalid(); }
    }
    private static void checkUsers(List<User> users) throws ApiFailure {
        if (users == null || users.size() > 20) { throw invalid(); }
        for (User user : users) { if (user == null || !user.valid()) { throw invalid(); } }
    }
    private static void checkId(String id) throws ApiFailure {
        if (!FriendModels.uuid(id)) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
    }
    private static void checkPage(int page) throws ApiFailure {
        if (page < 0 || page > 10000) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
    }
    private static ApiFailure invalid() { return new ApiFailure(ApiFailure.Kind.SERVER); }
}
