package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/register") Call<UserProfile> register(@Body RegisterRequest request);
    @POST("api/auth/login") Call<TokenResponse> login(@Body LoginRequest request);
    @POST("api/auth/refresh") Call<TokenResponse> refresh(@Body RefreshRequest request);
    @POST("api/auth/logout") Call<Void> logout(@Body RefreshRequest request);
}
