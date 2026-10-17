package com.taktak.app.data.repository;

import com.taktak.app.data.model.AuthModels.*;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.remote.ApiFailure;
import java.io.IOException;
import retrofit2.Response;

public final class AuthRepository {
    private final ApiClient client;
    public AuthRepository(ApiClient client) { this.client = client; }

    public UserProfile register(String email, String username, String name, String password) throws IOException {
        return checked(client.auth.register(new RegisterRequest(email, username, name, password)).execute());
    }

    public UserProfile login(String email, String password) throws IOException {
        long attempt = client.sessions.beginLogin();
        Response<TokenResponse> response = client.auth.login(new LoginRequest(email, password)).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        client.sessions.completeLogin(attempt, response.body());
        return profile();
    }

    public UserProfile profile() throws IOException { return checked(client.users.me().execute()); }

    public boolean hasSession() { return client.sessions.snapshot() != null; }

    public void logout() throws IOException { client.sessions.logout(); }
    public MomentRepository moments() { return new MomentRepository(client); }
    public FriendRepository friends() { return new FriendRepository(client); }

    private UserProfile checked(Response<UserProfile> response) throws IOException {
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        UserProfile profile = response.body();
        if (profile == null || !profile.isValid()) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        return profile;
    }
}
