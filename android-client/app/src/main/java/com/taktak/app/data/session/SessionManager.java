package com.taktak.app.data.session;

import com.taktak.app.data.model.AuthModels.RefreshRequest;
import com.taktak.app.data.model.AuthModels.TokenResponse;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.remote.AuthApi;
import java.io.IOException;
import retrofit2.Response;

public final class SessionManager {
    private final SessionStore store;
    private final AuthApi auth;
    private Session current;
    private long generation;
    private Session failedRefresh;
    private IOException refreshFailure;
    private long retryRefreshAfterNanos;
    private Runnable onCleared = () -> { };
    public synchronized void setOnCleared(Runnable listener) { onCleared = listener; }
    public synchronized boolean runIfCurrent(long expected, Runnable action) {
        if (current == null || current.generation != expected) { return false; }
        action.run(); return true;
    }
    public synchronized boolean runIfLoggedOut(Runnable action) {
        if (current != null) { return false; }
        action.run(); return true;
    }

    public SessionManager(SessionStore store, AuthApi auth) throws IOException {
        this.store = store; this.auth = auth; current = store.load();
        generation = current == null ? 0 : current.generation;
    }

    public synchronized Session snapshot() { return current; }

    public synchronized long beginLogin() throws IOException {
        clear();
        return generation;
    }

    public synchronized void completeLogin(long expectedGeneration, TokenResponse tokens) throws IOException {
        if (generation != expectedGeneration) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
        persist(tokens);
    }

    private void persist(TokenResponse tokens) throws IOException {
        if (tokens == null || !tokens.isValid()) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        Session next = new Session(tokens.accessToken, tokens.refreshToken,
                System.currentTimeMillis() + tokens.expiresIn * 1000, generation);
        try { store.save(next); current = next; failedRefresh = null; refreshFailure = null; }
        catch (IOException exception) { current = null; generation++; throw new ApiFailure(ApiFailure.Kind.STORAGE); }
    }

    public synchronized void clear() throws IOException {
        generation++;
        current = null;
        failedRefresh = null;
        refreshFailure = null;
        try { store.clear(); }
        catch (IOException exception) { throw new ApiFailure(ApiFailure.Kind.STORAGE); }
        finally { try { onCleared.run(); } catch (RuntimeException ignored) { } }
    }

    public synchronized void clearIfCurrent(Session sent) throws IOException {
        if (current == sent) { clear(); }
    }

    // Cùng khóa cho rotation/login/logout; các 401 cũ dùng lại token mới, không rotate lần nữa.
    public synchronized Session refresh(Session sent) throws IOException {
        if (current == null || sent == null || current.generation != sent.generation) { return null; }
        if (current != sent) { return current; }
        if (failedRefresh == sent && System.nanoTime() < retryRefreshAfterNanos) { throw refreshFailure; }
        try {
            Response<TokenResponse> response = auth.refresh(new RefreshRequest(current.refreshToken)).execute();
            if (!response.isSuccessful()) {
                ApiFailure failure = ApiFailure.from(response);
                if (response.code() == 401 || response.code() == 400) { clear(); }
                throw failure;
            }
            persist(response.body());
            return current;
        } catch (RuntimeException failure) {
            ApiFailure invalid = new ApiFailure(ApiFailure.Kind.SERVER);
            rememberFailure(sent, invalid);
            throw invalid;
        } catch (IOException failure) {
            rememberFailure(sent, failure);
            throw failure;
        }
    }

    private void rememberFailure(Session sent, IOException failure) {
        if (current == sent) {
            failedRefresh = sent;
            refreshFailure = failure;
            retryRefreshAfterNanos = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        }
    }

    // Hook cho logout ở 3C: đồng bộ với refresh và luôn xóa local dù mạng lỗi.
    public synchronized void logout() throws IOException {
        Session sent = current;
        try {
            if (sent != null) {
                Response<Void> response = auth.logout(new RefreshRequest(sent.refreshToken)).execute();
                if (!response.isSuccessful()) { throw ApiFailure.from(response); }
            }
        } finally { clear(); }
    }
}
