package com.taktak.app.data.model;

import com.google.gson.annotations.SerializedName;

public final class DeviceModels {
    private DeviceModels() { }

    public static final class DeviceRegistrationRequest {
        @SerializedName("deviceId")
        public final String deviceId;

        @SerializedName("fcmToken")
        public final String fcmToken;

        public DeviceRegistrationRequest(String deviceId, String fcmToken) {
            this.deviceId = deviceId;
            this.fcmToken = fcmToken;
        }
    }
}
