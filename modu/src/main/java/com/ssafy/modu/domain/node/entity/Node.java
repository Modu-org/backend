package com.ssafy.modu.domain.node.entity;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "node",
        indexes = {
                @Index(name = "idx_node_schedule", columnList = "schedule_id"),
                @Index(name = "idx_node_schedule_date_order", columnList = "schedule_id,visit_date,visit_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Column(name = "visit_order")
    private Integer visitOrder;

    @Column(name = "visit_date")
    private LocalDate visitDate;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Attraction 객체를 이용해서 Node 객체를 생성함
    public static Node from(Attraction attraction) {
        Node node = new Node();
        node.attraction = attraction;
        node.visitOrder = null;
        node.visitDate = null;
        return node;
    }

    public void updateVisitInfo(Integer visitOrder, LocalDate visitDate) {
        this.visitOrder = visitOrder;
        this.visitDate = visitDate;
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
