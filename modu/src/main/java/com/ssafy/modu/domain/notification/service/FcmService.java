package com.ssafy.modu.domain.notification.service;

import com.google.firebase.messaging.*;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenService fcmTokenService;

    // 토큰을 기반으로 바로 요청을 보내기 위한 test 코드
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

    // userId를 이용해서 해당 user의 FCM 토큰을 가져와서 알림을 전송하는 로직
    public void sendToUser(
            Long userId,
            String title,
            String body,
            Map<String, String> data
    ) {
        fcmTokenService.getActiveTokens(userId)
                .forEach(token -> sendSafely(userId, token, title, body, data));
    }

    private void sendSafely(
            Long userId,
            String token,
            String title,
            String body,
            Map<String, String> data
    ) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 사용자 알림 전송 실패. userId={}, errorCode={}, messagingErrorCode={}, message={}",
                    userId,
                    e.getErrorCode(),
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e
            );
            // 유효하지 않은 토큰이면 해당 토큰 비활성화
            if (isInvalidToken(e)) {
                fcmTokenService.deactivateToken(userId, token);
            }
        }
    }
    // 유효하지 않은 토큰인지 확인
    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT;
    }
}