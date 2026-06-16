package com.ssafy.modu.domain.routecache.entity;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "attraction_route_cache",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_route_cache_from_to_provider",
                        columnNames = {"from_attraction_id", "to_attraction_id", "provider"}
                )
        },
        indexes = {
                @Index(name = "idx_route_cache_from_to", columnList = "from_attraction_id,to_attraction_id"),
                @Index(name = "idx_route_cache_provider", columnList = "provider")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttractionRouteCache {

    public static final String PROVIDER_KAKAO = "KAKAO_MOBILITY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_cache_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_attraction_id", nullable = false)
    private Attraction fromAttraction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_attraction_id", nullable = false)
    private Attraction toAttraction;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AttractionRouteCache create(
            Attraction fromAttraction,
            Attraction toAttraction,
            String provider,
            Integer distanceMeters,
            Integer durationMinutes
    ) {
        AttractionRouteCache cache = new AttractionRouteCache();
        cache.fromAttraction = fromAttraction;
        cache.toAttraction = toAttraction;
        cache.provider = provider;
        cache.distanceMeters = distanceMeters;
        cache.durationMinutes = durationMinutes;
        cache.calculatedAt = LocalDateTime.now();
        return cache;
    }

    public void updateRoute(Integer distanceMeters, Integer durationMinutes) {
        this.distanceMeters = distanceMeters;
        this.durationMinutes = durationMinutes;
        this.calculatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}