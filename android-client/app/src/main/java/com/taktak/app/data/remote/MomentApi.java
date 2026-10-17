package com.taktak.app.data.remote;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface MomentApi {
    final class CreatedMoment { public String id; }
    @Multipart @POST("api/moments")
    Call<CreatedMoment> upload(@Part MultipartBody.Part image, @Part("recipientIds") RequestBody recipients);
    @Streaming @GET("api/moments/{id}/image")
    Call<ResponseBody> image(@Path("id") String id);
}
