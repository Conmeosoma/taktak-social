package com.taktak.app.data.remote;

import com.taktak.app.data.model.FriendModels.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface FriendApi {
    @GET("api/users/search") Call<List<User>> search(@Query("q") String query);
    @GET("api/friends/requests") Call<Page<Invitation>> incoming(@Query("page") int page);
    @GET("api/friends") Call<Page<User>> friends(@Query("page") int page);
    @POST("api/friends/requests") Call<Invitation> send(@Body Send request);
    @PUT("api/friends/requests/{id}") Call<Invitation> respond(@Path("id") String id, @Body Respond request);
    @DELETE("api/friends/{id}") Call<Void> unfriend(@Path("id") String id);
    @POST("api/blocks/{id}") Call<Void> block(@Path("id") String id);
}
