package com.taktak.app.data.remote;

import com.taktak.app.data.model.AuthModels.UserProfile;
import retrofit2.Call;
import retrofit2.http.GET;

public interface UserApi {
    @GET("api/users/me") Call<UserProfile> me();
}
