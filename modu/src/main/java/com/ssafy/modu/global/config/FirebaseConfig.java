package com.ssafy.modu.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.client-email}")
    private String clientEmail;

    @Value("${firebase.private-key}")
    private String privateKey;

    @Value("${firebase.private-key-id}")
    private String privateKeyId;

    @Value("${firebase.client-id}")
    private String clientId;

    @Value("${firebase.token-uri}")
    private String tokenUri;

    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp already initialized.");
                return;
            }

            log.info("Firebase config loaded. projectId={}, clientEmail={}",
                    projectId,
                    clientEmail
            );

            String normalizedPrivateKey = privateKey.replace("\\n", "\n");

            String serviceAccountJson = """
                    {
                      "type": "service_account",
                      "project_id": "%s",
                      "private_key_id": "%s",
                      "private_key": "%s",
                      "client_email": "%s",
                      "client_id": "%s",
                      "token_uri": "%s"
                    }
                    """.formatted(
                    projectId,
                    privateKeyId,
                    normalizedPrivateKey.replace("\n", "\\n"),
                    clientEmail,
                    clientId,
                    tokenUri
            );

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);

            log.info("FirebaseApp initialized successfully. projectId={}, name={}",
                    projectId,
                    app.getName()
            );

        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            throw new IllegalStateException("Firebase 초기화에 실패했습니다.", e);
        }
    }
}