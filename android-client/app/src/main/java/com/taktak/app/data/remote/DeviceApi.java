package com.taktak.app.data.remote;

import com.taktak.app.data.model.DeviceModels.DeviceRegistrationRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DeviceApi {
    @POST("api/devices")
    Call<Void> registerDevice(@Body DeviceRegistrationRequest request);

    @DELETE("api/devices/{deviceId}")
    Call<Void> unregisterDevice(@Path("deviceId") String deviceId);
}
