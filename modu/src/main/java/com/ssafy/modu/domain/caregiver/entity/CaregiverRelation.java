package com.ssafy.modu.domain.caregiver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "caregiver_relation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_caregiver_relation_traveler_caregiver",
                        columnNames = {"traveler_id", "caregiver_id"}
                )
        },
        indexes = {
                @Index(name = "idx_caregiver_relation_traveler", columnList = "traveler_id"),
                @Index(name = "idx_caregiver_relation_caregiver", columnList = "caregiver_id")
        }
)
public class CaregiverRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "caregiver_relation_id")
    private Long id;

    @Column(name = "traveler_id", nullable = false)
    private Long travelerId;

    @Column(name = "caregiver_id", nullable = false)
    private Long caregiverId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CaregiverRelation create(Long travelerId, Long caregiverId) {
        CaregiverRelation relation = new CaregiverRelation();
        relation.travelerId = travelerId;
        relation.caregiverId = caregiverId;
        relation.active = true;

        LocalDateTime now = LocalDateTime.now();
        relation.createdAt = now;
        relation.acceptedAt = now;
        relation.updatedAt = now;

        return relation;
    }

    public void activate() {
        this.active = true;
        this.acceptedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}