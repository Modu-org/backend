package com.ssafy.modu.domain.notification.entity;

import com.ssafy.modu.domain.notification.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_receiver", columnList = "receiver_id"),
                @Index(name = "idx_notification_receiver_read", columnList = "receiver_id, read_status"),
                @Index(name = "idx_notification_created_at", columnList = "created_at")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "read_status", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification create(
            Long receiverId,
            NotificationType type,
            String title,
            String body,
            Long referenceId
    ) {
        Notification notification = new Notification();
        notification.receiverId = receiverId;
        notification.type = type;
        notification.title = title;
        notification.body = body;
        notification.referenceId = referenceId;
        notification.read = false;
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    public void markAsRead() {
        this.read = true;
    }
}