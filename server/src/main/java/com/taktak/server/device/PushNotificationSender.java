package com.taktak.server.device;

import java.util.Collection;
import java.util.Map;

public interface PushNotificationSender {
    enum Delivery { ACCEPTED, RETRY, UNREGISTERED, DISABLED, REJECTED }
    Delivery deliver(String fcmToken, Map<String, String> data);
    void sendDataMessage(String fcmToken, Map<String, String> data);
    void sendDataMessageToDevices(Collection<String> fcmTokens, Map<String, String> data);
}
