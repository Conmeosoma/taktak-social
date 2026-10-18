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
        var devices = mock(DeviceTokenRepository.class);
        var sender = new FirebasePushNotificationSender(null, devices);
        for (int i=0;i<1000;i++) { sender.sendDataMessage("fixture-not-real", Map.of("type", "moment_created")); }
        verifyNoInteractions(devices);
    }
}
