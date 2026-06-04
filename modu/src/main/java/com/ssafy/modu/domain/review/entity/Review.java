package com.ssafy.modu.domain.review.entity;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "review",
        indexes = {
                @Index(name = "idx_review_attraction_created", columnList = "attraction_id, created_at"),
                @Index(name = "idx_review_user_created", columnList = "user_id, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_attraction",
                        columnNames = {"user_id", "attraction_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_private", nullable = false)
    private boolean privateReview;

    @Column(nullable = false)
    private int rate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Review(User user, Attraction attraction, String content, boolean privateReview, int rate) {
        this.user = user;
        this.attraction = attraction;
        this.content = content;
        this.privateReview = privateReview;
        this.rate = rate;
    }

    public void update(String content, boolean privateReview, int rate) {
        this.content = content;
        this.privateReview = privateReview;
        this.rate = rate;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}