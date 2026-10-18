package com.taktak.server;

import com.taktak.server.device.PushNotificationSender;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Chỉ tồn tại trong test classpath, không đóng gói vào JAR production. */
final class RecordingPushSender implements PushNotificationSender {
    record SentMessage(String fcmToken, Map<String, String> data) { }
    private final List<SentMessage> messages = new CopyOnWriteArrayList<>();
    List<SentMessage> getRecordedMessages() { return List.copyOf(messages); }
    void clearRecordedMessages() { messages.clear(); }
    @Override public void sendDataMessage(String token, Map<String,String> data) {
        messages.add(new SentMessage(token, Map.copyOf(data)));
    }
    @Override public void sendDataMessageToDevices(Collection<String> tokens, Map<String,String> data) {
        tokens.forEach(token -> sendDataMessage(token, data));
    }
}
