package com.ssafy.modu.domain.user.entity;

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

    @Column(name = "physical", nullable = false)
    private Boolean physical;

    @Column(name = "infant_family", nullable = false)
    private Boolean infantFamily;

    @Column(name = "visual", nullable = false)
    private Boolean visual;

    @Column(name = "hearing", nullable = false)
    private Boolean hearing;

    void setUser(User user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if (this.physical == null) {
            this.physical = false;
        }

        if (this.infantFamily == null) {
            this.infantFamily = false;
        }

        if (this.visual == null) {
            this.visual = false;
        }

        if (this.hearing == null) {
            this.hearing = false;
        }
    }

    public void updateDetail(
            Boolean physical,
            Boolean infantFamily,
            Boolean visual,
            Boolean hearing
    ) {
        this.physical = physical;
        this.infantFamily = infantFamily;
        this.visual = visual;
        this.hearing = hearing;
    }
}