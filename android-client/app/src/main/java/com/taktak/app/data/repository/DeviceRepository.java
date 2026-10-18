package com.taktak.app.data.repository;

import com.taktak.app.data.model.DeviceModels.DeviceRegistrationRequest;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.session.DeviceIdProvider;
import retrofit2.Response;

import java.io.IOException;

public class DeviceRepository {
    private final ApiClient api;
    private final DeviceIdProvider deviceIdProvider;

    public DeviceRepository(ApiClient api, DeviceIdProvider deviceIdProvider) {
        this.api = api;
        this.deviceIdProvider = deviceIdProvider;
    }

    public void register(String fcmToken) throws IOException {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        String deviceId = deviceIdProvider.getDeviceId();
        try {
            Response<Void> res = api.devices.registerDevice(new DeviceRegistrationRequest(deviceId, fcmToken.trim())).execute();
            if (!res.isSuccessful() && res.code() != 204) {
                throw ApiFailure.from(res);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public void unregister() {
        String deviceId = deviceIdProvider.getDeviceId();
        try {
            Response<Void> res = api.devices.unregisterDevice(deviceId).execute();
            // Best-effort unregister on logout
        } catch (Exception ignored) {
            // Unregister is best effort during sign-out
        }
    }

    public String currentDeviceId() {
        return deviceIdProvider.getDeviceId();
    }
}
