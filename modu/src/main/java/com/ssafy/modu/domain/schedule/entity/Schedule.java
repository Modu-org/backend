package com.ssafy.modu.domain.schedule.entity;

import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitDate ASC, visitOrder ASC, id ASC")
    private List<Node> nodes = new ArrayList<>();

    public static Schedule create(User user, String title, LocalDate startDate, LocalDate endDate) {
        Schedule schedule = new Schedule();
        schedule.user = user;
        schedule.title = normalizeTitle(title);
        schedule.startDate = startDate;
        schedule.endDate = endDate;
        return schedule;
    }

    public void update(String title, LocalDate startDate, LocalDate endDate) {
        this.title = normalizeTitle(title);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void addNode(Node node) {
        nodes.add(node);
        node.setSchedule(this);
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "제목 없음";
        }
        return title.trim();
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
