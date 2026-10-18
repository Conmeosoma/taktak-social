package com.taktak.app.data.remote;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface MomentApi {
    final class CreatedMoment { public String id; }
    @Multipart @POST("api/moments")
    Call<CreatedMoment> upload(
            @Header("Idempotency-Key") String idempotencyKey,
            @Part MultipartBody.Part image,
            @Part("recipientIds") RequestBody recipients);

    @Multipart @POST("api/moments")
    Call<CreatedMoment> upload(
            @Header("Idempotency-Key") String idempotencyKey,
            @Part MultipartBody.Part image,
            @Part("recipientIds") RequestBody recipients,
            @Part("caption") RequestBody caption);

    @Multipart @POST("api/moments")
    Call<CreatedMoment> uploadToFriends(
            @Header("Idempotency-Key") String idempotencyKey,
            @Part MultipartBody.Part image,
            @Part("recipientIds") java.util.List<RequestBody> recipients,
            @Part("caption") RequestBody caption);

    @Streaming @GET("api/moments/{id}/image")
    Call<ResponseBody> image(@Path("id") String id);

    @GET("api/moments")
    Call<com.taktak.app.data.model.MomentModels.MomentsPage> listMoments(
            @Query("type") String type,
            @Query("page") int page);

    @DELETE("api/moments/{id}")
    Call<Void> deleteMoment(@Path("id") String id);

    @GET("api/moments/latest")
    Call<com.taktak.app.data.model.MomentModels.MomentItem> latest();
}
