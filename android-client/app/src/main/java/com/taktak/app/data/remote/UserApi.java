package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.UserProfile;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.*;
import okhttp3.*;

public interface UserApi {
    @GET("api/users/me") Call<UserProfile> me();
    @PUT("api/users/me") Call<UserProfile> update(@Body com.taktak.app.data.model.AuthModels.UpdateProfileRequest request);
    @Multipart @POST("api/users/me/avatar") Call<UserProfile> uploadAvatar(@Part MultipartBody.Part image);
    @GET("api/users/{id}/avatar") Call<ResponseBody> avatar(@Path("id") String id);
}
