package com.taktak.app.data.remote;

import com.taktak.app.data.model.AiModels.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface AiApi {
    @GET("api/ai/status") Call<Status> status();
    @GET("api/ai/conversations") Call<Page<Conversation>> conversations(@Query("page") int page);
    @POST("api/ai/conversations") Call<Conversation> create();
    @GET("api/ai/conversations/{id}/messages") Call<Page<Message>> messages(@Path("id") String id, @Query("page") int page);
    @DELETE("api/ai/conversations/{id}") Call<Void> delete(@Path("id") String id);
    @POST("api/ai/chat") Call<Message> send(@Body Send request);
}
