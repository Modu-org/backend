package com.ssafy.modu.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "fcm_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fcm_token_user_token",
                        columnNames = {"user_id", "token"}
                )
        }
)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String token;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static FcmToken create(Long userId, String token) {
        FcmToken fcmToken = new FcmToken();
        fcmToken.userId = userId;
        fcmToken.token = token;
        fcmToken.active = true;
        fcmToken.createdAt = LocalDateTime.now();
        fcmToken.updatedAt = LocalDateTime.now();
        return fcmToken;
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}