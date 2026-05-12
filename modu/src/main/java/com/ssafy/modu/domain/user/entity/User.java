package com.ssafy.modu.domain.user.entity;

import com.ssafy.modu.domain.user.enums.UiMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "user_name", nullable = false, length = 30, unique = true)
    private String userName;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "profile_img", length = 255)
    private String profileImg;

    @Column(name = "uses_wheelchair")
    private Boolean usesWheelchair;

    @Column(name = "has_stroller")
    private Boolean hasStroller;

    @Column(name = "uses_walking_aid")
    private Boolean usesWalkingAid;

    @Column(name = "has_service_dog")
    private Boolean hasServiceDog;

    @Column(name = "needs_visual_assistance")
    private Boolean needsVisualAssistance;

    @Column(name = "age_group_code")
    private Integer ageGroupCode;

    @Column(name = "trip_style_code")
    private Integer tripStyleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_mode", nullable = false, length = 10)
    private UiMode uiMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.uiMode == null) {
            this.uiMode = UiMode.STANDARD;
        }

        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(
            String nickname,
            Integer ageGroupCode,
            Integer tripStyleCode,
            Boolean usesWheelchair,
            Boolean hasStroller,
            Boolean usesWalkingAid,
            Boolean hasServiceDog,
            Boolean needsVisualAssistance,
            UiMode uiMode
    ) {
        this.nickname = nickname;
        this.ageGroupCode = ageGroupCode;
        this.tripStyleCode = tripStyleCode;
        this.usesWheelchair = usesWheelchair;
        this.hasStroller = hasStroller;
        this.usesWalkingAid = usesWalkingAid;
        this.hasServiceDog = hasServiceDog;
        this.needsVisualAssistance = needsVisualAssistance;
        this.uiMode = uiMode;
    }
}