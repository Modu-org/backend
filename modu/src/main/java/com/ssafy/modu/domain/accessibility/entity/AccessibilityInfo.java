package com.ssafy.modu.domain.accessibility.entity;

import com.ssafy.modu.domain.accessibility.entity.enums.*;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "accessibility_info",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accessibility_source_field",
                        columnNames = {"attraction_id", "source", "source_field"}
                )
        },
        indexes = {
                @Index(name = "idx_accessibility_filter", columnList = "type,status"),
                @Index(name = "idx_accessibility_category", columnList = "category,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessibilityInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accessibility_info_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 100)
    private AccessibilitySource source;

    @Column(name = "source_field", nullable = false, length = 50)
    private String sourceField;

    @Lob
    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;

    @Column(name = "source_priority", nullable = false)
    private int sourcePriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private AccessibilityCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AccessibilityType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccessibilityStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AccessibilityInfo(
            Attraction attraction,
            AccessibilitySource source,
            String sourceField,
            String rawValue,
            AccessibilityCategory category,
            AccessibilityType type,
            AccessibilityStatus status
    ) {
        this.attraction = attraction;
        this.source = source;
        this.sourceField = sourceField;
        this.rawValue = normalizeRawValue(rawValue);
        this.sourcePriority = source.getPriority();
        this.category = category;
        this.type = type;
        this.status = status;
    }

    public static AccessibilityInfo create(
            Attraction attraction,
            AccessibilitySource source,
            String sourceField,
            String rawValue,
            AccessibilityCategory category,
            AccessibilityType type,
            AccessibilityStatus status
    ) {
        return new AccessibilityInfo(attraction, source, sourceField, rawValue, category, type, status);
    }

    public void update(String rawValue, AccessibilityCategory category, AccessibilityType type, AccessibilityStatus status) {
        this.rawValue = normalizeRawValue(rawValue);
        this.category = category;
        this.type = type;
        this.status = status;
        this.sourcePriority = source.getPriority();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private static String normalizeRawValue(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }
}
