package com.taktak.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class AppDeviceIdProvider implements DeviceIdProvider {
    private static final String PREF_NAME = "taktak_device_identity";
    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences preferences;

    public AppDeviceIdProvider(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public synchronized String getDeviceId() {
        String deviceId = preferences.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            preferences.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }
}
