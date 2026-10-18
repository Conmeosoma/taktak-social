package com.taktak.server.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class DeviceDtos {
    private DeviceDtos() { }

    public record DeviceRegistrationRequest(
            @NotBlank(message = "DEVICE_ID_REQUIRED")
            @Size(max = 255, message = "DEVICE_ID_TOO_LONG")
            String deviceId,

            @NotBlank(message = "FCM_TOKEN_REQUIRED")
            @Size(max = 4096, message = "FCM_TOKEN_TOO_LONG")
            String fcmToken
    ) { }

    public record DeviceItem(
            UUID id,
            String deviceId,
            Instant updatedAt
    ) { }
}
