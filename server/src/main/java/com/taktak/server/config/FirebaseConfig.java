package com.taktak.server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    @Value("${taktak.fcm.credentials-path:}")
    private String credentialsPath;

    @Bean(destroyMethod = "delete")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "taktak.fcm", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("FCM enabled but credentials are not configured.");
        }
        try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                return FirebaseApp.initializeApp(options, "taktak-" + java.util.UUID.randomUUID());
        } catch (Exception e) {
            throw new IllegalStateException("FCM enabled but credentials could not be loaded.");
        }
    }
}
