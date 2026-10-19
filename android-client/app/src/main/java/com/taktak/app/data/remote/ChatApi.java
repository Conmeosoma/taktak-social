package com.taktak.app.data.remote;

import com.taktak.app.data.model.ChatModels.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ChatApi {
    @GET("api/conversations") Call<Page<Conversation>> inbox(@Query("page") int page);
    @POST("api/conversations/with-friend/{id}") Call<Conversation> withFriend(@Path("id") String id);
    @GET("api/conversations/{id}") Call<Conversation> conversation(@Path("id") String id);
    @GET("api/conversations/{id}/messages") Call<Page<Message>> messages(@Path("id") String id, @Query("page") int page);
    @POST("api/conversations/{id}/messages") Call<Message> send(@Path("id") String id, @Body Send request);
    @PUT("api/conversations/{id}/read") Call<Void> read(@Path("id") String id, @Body Read request);
}
