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

    @Override
    public void sendDataMessage(String fcmToken, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank() || data == null || data.isEmpty()) {
            return;
        }
        if (firebaseApp == null) {
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(data)
                    .build();
            FirebaseMessaging.getInstance(firebaseApp).send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push failed. ErrorCode: {}", e.getMessagingErrorCode());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Token has expired or app was uninstalled: clean up stale token
                try {
                    deviceTokenRepository.deleteByFcmToken(fcmToken);
                } catch (Exception ex) {
                    log.warn("Failed to remove expired device registration.");
                }
            }
        } catch (Exception e) {
            log.warn("Unexpected error sending FCM message.");
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
