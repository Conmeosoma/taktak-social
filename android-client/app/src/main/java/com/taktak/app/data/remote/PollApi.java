package com.taktak.app.data.remote;

import com.taktak.app.data.model.PollModels.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface PollApi {
    @POST("api/moments/{id}/poll")
    Call<PollResponse> createPoll(@Path("id") String momentId, @Body CreatePollRequest request);

    @GET("api/moments/{id}/poll")
    Call<PollResponse> getPoll(@Path("id") String momentId);

    @POST("api/moments/{id}/poll/vote")
    Call<PollResponse> vote(@Path("id") String momentId, @Body VoteRequest request);

    @POST("api/moments/{id}/poll/close")
    Call<PollResponse> close(@Path("id") String momentId);
}

