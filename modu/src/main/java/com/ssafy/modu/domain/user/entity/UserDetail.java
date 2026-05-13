package com.ssafy.modu.domain.user.entity;

import com.ssafy.modu.domain.user.enums.UiMode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserDetail {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

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

    void setUser(User user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if (this.uiMode == null) {
            this.uiMode = UiMode.STANDARD;
        }
    }

    public void updateDetail(
            Integer ageGroupCode,
            Integer tripStyleCode,
            Boolean usesWheelchair,
            Boolean hasStroller,
            Boolean usesWalkingAid,
            Boolean hasServiceDog,
            Boolean needsVisualAssistance,
            UiMode uiMode
    ) {
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