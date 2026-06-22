package com.ssafy.modu.domain.notification.service;

import com.ssafy.modu.domain.notification.dto.response.NotificationResponse;
import com.ssafy.modu.domain.notification.entity.Notification;
import com.ssafy.modu.domain.notification.entity.enums.NotificationType;
import com.ssafy.modu.domain.notification.repository.NotificationRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(
            Long receiverId,
            NotificationType type,
            String title,
            String body,
            Long referenceId
    ) {
        return notificationRepository.save(
                Notification.create(
                        receiverId,
                        type,
                        title,
                        body,
                        referenceId
                )
        );
    }

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }
}