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

    public FirebasePushNotificationSender(
            @Autowired(required = false) FirebaseApp firebaseApp
    ) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    public void sendDataMessage(String fcmToken, Map<String, String> data) {
        deliver(fcmToken, data);
    }

    @Override
    public Delivery deliver(String fcmToken, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank() || data == null || data.isEmpty()) {
            return Delivery.REJECTED;
        }
        if (firebaseApp == null) {
            return Delivery.DISABLED;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(data)
                    .build();
            FirebaseMessaging.getInstance(firebaseApp).send(message);
            return Delivery.ACCEPTED;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push failed. ErrorCode: {}", e.getMessagingErrorCode());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Dispatcher xóa có ràng buộc recipient; sender không xóa token toàn cục.
                return Delivery.UNREGISTERED;
            }
            return e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT
                    || e.getMessagingErrorCode() == MessagingErrorCode.SENDER_ID_MISMATCH
                    ? Delivery.REJECTED : Delivery.RETRY;
        } catch (Exception e) {
            log.warn("Unexpected error sending FCM message.");
            return Delivery.RETRY;
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
