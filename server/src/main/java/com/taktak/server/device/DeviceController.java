package com.taktak.server.device;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceDtos.DeviceRegistrationRequest request,
            JwtAuthenticationToken jwt
    ) {
        UUID userId = UUID.fromString(jwt.getName());
        deviceService.registerDevice(userId, request.deviceId(), request.fcmToken());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(
            @PathVariable("deviceId") String deviceId,
            JwtAuthenticationToken jwt
    ) {
        UUID userId = UUID.fromString(jwt.getName());
        deviceService.unregisterDevice(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<DeviceDtos.DeviceItem>> listDevices(JwtAuthenticationToken jwt) {
        UUID userId = UUID.fromString(jwt.getName());
        return ResponseEntity.ok(deviceService.listDevices(userId));
    }
}
