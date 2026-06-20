package com.ssafy.modu.domain.notification.service;

import com.google.firebase.messaging.*;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenService fcmTokenService;

    public String sendNotification(
            String targetToken,
            String title,
            String body
    ) {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);

            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패. errorCode={}, messagingErrorCode={}, message={}",
                    e.getErrorCode(),
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e
            );

            throw new BusinessException(ErrorCode.FCM_SEND_FAILED);
        }
    }

    public void sendToUser(
            Long userId,
            String title,
            String body
    ) {
        fcmTokenService.getActiveTokens(userId)
                .forEach(token -> sendSafely(userId, token, title, body));
    }

    private void sendSafely(
            Long userId,
            String token,
            String title,
            String body
    ) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 사용자 알림 전송 실패. userId={}, errorCode={}, messagingErrorCode={}, message={}",
                    userId,
                    e.getErrorCode(),
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e
            );

            if (isInvalidToken(e)) {
                fcmTokenService.deactivateToken(userId, token);
                log.info("유효하지 않은 FCM 토큰 비활성화. userId={}", userId);
            }
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT;
    }
}