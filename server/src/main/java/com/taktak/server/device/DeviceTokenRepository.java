package com.taktak.server.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByUserIdAndDeviceId(UUID userId, String deviceId);
    Optional<DeviceToken> findByFcmToken(String fcmToken);
    List<DeviceToken> findAllByUserId(UUID userId);
    List<DeviceToken> findAllByUserIdIn(Collection<UUID> userIds);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM DeviceToken d WHERE d.userId = :userId AND d.deviceId = :deviceId")
    void deleteByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);
}
