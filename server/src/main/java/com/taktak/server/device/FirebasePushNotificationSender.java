package com.taktak.server.device;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class FirebasePushNotificationSender implements PushNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(FirebasePushNotificationSender.class);
    private final FirebaseApp firebaseApp;
    private final DeviceTokenRepository deviceTokenRepository;

    public FirebasePushNotificationSender(
            @Autowired(required = false) FirebaseApp firebaseApp,
            DeviceTokenRepository deviceTokenRepository
    ) {
        this.firebaseApp = firebaseApp;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public record SentMessage(String fcmToken, Map<String, String> data) {}
    private final java.util.List<SentMessage> recordedMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

    public java.util.List<SentMessage> getRecordedMessages() { return java.util.List.copyOf(recordedMessages); }
    public void clearRecordedMessages() { recordedMessages.clear(); }

    @Override
    public void sendDataMessage(String fcmToken, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank() || data == null || data.isEmpty()) {
            return;
        }
        recordedMessages.add(new SentMessage(fcmToken, Map.copyOf(data)));
        if (firebaseApp == null) {
            log.debug("FirebaseApp not configured. Skipping push message to token: {}", fcmToken);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(data)
                    .build();
            FirebaseMessaging.getInstance(firebaseApp).send(message);
            log.debug("Sent FCM data message to token: {}", fcmToken);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push failed for token: {}. ErrorCode: {}", fcmToken, e.getMessagingErrorCode());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                // Token has expired or app was uninstalled: clean up stale token
                log.info("Removing stale FCM token: {}", fcmToken);
                try {
                    deviceTokenRepository.deleteByFcmToken(fcmToken);
                } catch (Exception ex) {
                    log.warn("Failed to delete stale token {}: {}", fcmToken, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Unexpected error sending FCM message: {}", e.getMessage());
        }
    }

    @Override
    public void sendDataMessageToDevices(Collection<String> fcmTokens, Map<String, String> data) {
        if (fcmTokens == null || fcmTokens.isEmpty() || data == null || data.isEmpty()) {
            return;
        }
        for (String token : fcmTokens) {
            sendDataMessage(token, data);
        }
    }
}
