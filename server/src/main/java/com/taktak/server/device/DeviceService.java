package com.taktak.server.device;

import com.taktak.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerDevice(UUID userId, String deviceId, String fcmToken) {
        if (userId == null) {
            throw new DeviceFailure(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        if (!userRepository.existsById(userId)) {
            throw new DeviceFailure(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new DeviceFailure(HttpStatus.BAD_REQUEST, "DEVICE_ID_REQUIRED");
        }
        if (deviceId.length() > 255) {
            throw new DeviceFailure(HttpStatus.BAD_REQUEST, "DEVICE_ID_TOO_LONG");
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new DeviceFailure(HttpStatus.BAD_REQUEST, "FCM_TOKEN_REQUIRED");
        }
        if (fcmToken.length() > 4096) {
            throw new DeviceFailure(HttpStatus.BAD_REQUEST, "FCM_TOKEN_TOO_LONG");
        }

        String cleanDeviceId = deviceId.trim();
        String cleanToken = fcmToken.trim();
        Instant now = Instant.now();

        // 1. If this fcm_token is currently held by any record other than (userId, cleanDeviceId), remove it to honor UNIQUE constraint
        var existingByToken = deviceTokenRepository.findByFcmToken(cleanToken);
        if (existingByToken.isPresent()) {
            DeviceToken dt = existingByToken.get();
            if (!dt.getUserId().equals(userId) || !dt.getDeviceId().equals(cleanDeviceId)) {
                deviceTokenRepository.delete(dt);
                deviceTokenRepository.flush();
            }
        }

        // 2. Upsert by (userId, cleanDeviceId)
        var existingDevice = deviceTokenRepository.findByUserIdAndDeviceId(userId, cleanDeviceId);
        if (existingDevice.isPresent()) {
            DeviceToken dt = existingDevice.get();
            dt.setFcmToken(cleanToken);
            dt.setUpdatedAt(now);
            deviceTokenRepository.saveAndFlush(dt);
        } else {
            DeviceToken newDevice = new DeviceToken(UUID.randomUUID(), userId, cleanDeviceId, cleanToken, now);
            deviceTokenRepository.saveAndFlush(newDevice);
        }
    }

    @Transactional
    public void unregisterDevice(UUID userId, String deviceId) {
        if (userId == null) {
            throw new DeviceFailure(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        deviceTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId.trim());
    }

    @Transactional(readOnly = true)
    public List<DeviceDtos.DeviceItem> listDevices(UUID userId) {
        if (userId == null) {
            throw new DeviceFailure(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        return deviceTokenRepository.findAllByUserId(userId).stream()
                .map(d -> new DeviceDtos.DeviceItem(d.getId(), d.getDeviceId(), d.getUpdatedAt()))
                .toList();
    }
}
