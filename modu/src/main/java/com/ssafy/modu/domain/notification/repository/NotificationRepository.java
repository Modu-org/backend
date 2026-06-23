package com.ssafy.modu.domain.notification.repository;

import com.ssafy.modu.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    Optional<Notification> findByIdAndReceiverId(Long notificationId, Long receiverId);

    long countByReceiverIdAndReadFalse(Long receiverId);
}