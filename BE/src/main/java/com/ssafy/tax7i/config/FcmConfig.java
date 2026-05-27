package com.ssafy.tax7i.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmConfig {

    @Value("${fcm.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() throws IOException {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FCM service account path가 설정되지 않았습니다. Firebase 초기화를 건너뜁니다.");
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath)))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase 초기화 완료");
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()
                || FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase가 초기화되지 않아 FirebaseMessaging 빈을 생성하지 않습니다.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
