package com.taktak.server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${taktak.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            InputStream serviceAccount = null;
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                File file = new File(credentialsPath);
                if (file.exists() && file.isFile()) {
                    serviceAccount = new FileInputStream(file);
                    log.info("Loading Firebase credentials from file: {}", credentialsPath);
                }
            }

            if (serviceAccount == null) {
                String envVar = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (envVar != null && !envVar.isBlank()) {
                    File envFile = new File(envVar);
                    if (envFile.exists() && envFile.isFile()) {
                        serviceAccount = new FileInputStream(envFile);
                        log.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", envVar);
                    }
                }
            }

            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                return FirebaseApp.initializeApp(options);
            } else {
                log.info("No Firebase Admin credentials provided. Push notifications will be mocked or skipped in local/test mode.");
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to initialize FirebaseApp with credentials: {}. Falling back to uninitialized state.", e.getMessage());
            return null;
        }
    }
}
