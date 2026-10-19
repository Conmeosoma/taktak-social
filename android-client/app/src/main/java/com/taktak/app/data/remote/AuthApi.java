package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApi {
    @GET("api/auth/check-username") Call<UsernameAvailabilityResponse> checkUsername(@Query("username") String username);
    @POST("api/auth/register") Call<UserProfile> register(@Body RegisterRequest request);
    @POST("api/auth/login") Call<TokenResponse> login(@Body LoginRequest request);
    @POST("api/auth/refresh") Call<TokenResponse> refresh(@Body RefreshRequest request);
    @POST("api/auth/logout") Call<Void> logout(@Body RefreshRequest request);
    @POST("api/auth/forgot-password") Call<com.taktak.app.data.model.RecoveryModels.Notice> forgot(@Body com.taktak.app.data.model.RecoveryModels.Email request);
    @POST("api/auth/verification/request") Call<com.taktak.app.data.model.RecoveryModels.Notice> requestVerification(@Body LoginRequest request);
    @POST("api/auth/verification/confirm") Call<com.taktak.app.data.model.RecoveryModels.Notice> verify(@Body com.taktak.app.data.model.RecoveryModels.Verification request);
    @POST("api/auth/reset-password") Call<com.taktak.app.data.model.RecoveryModels.Notice> reset(@Body com.taktak.app.data.model.RecoveryModels.Reset request);
}
