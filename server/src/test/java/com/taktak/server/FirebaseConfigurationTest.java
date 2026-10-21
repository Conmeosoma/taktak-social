package com.taktak.server;

import com.google.firebase.FirebaseApp;
import com.taktak.server.config.FirebaseConfig;
import com.taktak.server.device.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(FirebaseConfig.class);
    @Test void disabledDoesNotLoadCredentialsOrCreateApp() {
        runner.withPropertyValues("taktak.fcm.enabled=false", "taktak.fcm.credentials-path=missing-file")
                .run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean(FirebaseApp.class); });
    }
    @Test void enabledWithoutCredentialsFailsInsteadOfSilentlyDroppingPush() {
        runner.withPropertyValues("taktak.fcm.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test void configuredPathIsBoundAndInvalidCredentialsFailFast() {
        runner.withPropertyValues("taktak.fcm.enabled=true", "taktak.fcm.credentials-path=missing-file")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage("FCM enabled but credentials could not be loaded."));
    }
    @Test void productionSenderHasNoMessageRecorderAndDisabledSendHasNoSideEffects() {
        assertThat(Arrays.stream(FirebasePushNotificationSender.class.getDeclaredFields())
                .noneMatch(field -> Collection.class.isAssignableFrom(field.getType()))).isTrue();
        var sender = new FirebasePushNotificationSender(null);
        assertThat(sender.deliver("fixture-not-real",Map.of("type","moment_created")))
                .isEqualTo(PushNotificationSender.Delivery.DISABLED);
        for (int i=0;i<1000;i++) { sender.sendDataMessage("fixture-not-real", Map.of("type", "moment_created")); }
        assertThat(Arrays.stream(FirebasePushNotificationSender.class.getDeclaredFields())
                .noneMatch(field -> DeviceTokenRepository.class.isAssignableFrom(field.getType()))).isTrue();
    }

    @Test void unregisteredIsReturnedWithoutGlobalRegistrationMutation() throws Exception {
        var app=mock(FirebaseApp.class);
        var messaging=mock(com.google.firebase.messaging.FirebaseMessaging.class);
        var failure=mock(com.google.firebase.messaging.FirebaseMessagingException.class);
        when(failure.getMessagingErrorCode()).thenReturn(com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(com.google.firebase.messaging.Message.class))).thenThrow(failure);
        try(var factory=mockStatic(com.google.firebase.messaging.FirebaseMessaging.class)) {
            factory.when(()->com.google.firebase.messaging.FirebaseMessaging.getInstance(app)).thenReturn(messaging);
            assertThat(new FirebasePushNotificationSender(app).deliver("QA-fixture",Map.of("type","moment_created")))
                    .isEqualTo(PushNotificationSender.Delivery.UNREGISTERED);
        }
    }
}
