package com.ssafy.modu.domain.edge.entity;

import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "edge",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_edge_schedule_from_to",
                        columnNames = {"schedule_id", "from_node_id", "to_node_id"}
                )
        },
        indexes = {
                @Index(name = "idx_edge_schedule", columnList = "schedule_id"),
                @Index(name = "idx_edge_from_node", columnList = "from_node_id"),
                @Index(name = "idx_edge_to_node", columnList = "to_node_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Edge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edge_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", nullable = false)
    private Node fromNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", nullable = false)
    private Node toNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "estimated_time_minutes", nullable = false)
    private Integer estimatedTimeMinutes;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Edge create(
            Schedule schedule,
            Node fromNode,
            Node toNode,
            Integer estimatedTimeMinutes,
            Integer distanceMeters
    ) {
        Edge edge = new Edge();
        edge.schedule = schedule;
        edge.fromNode = fromNode;
        edge.toNode = toNode;
        edge.estimatedTimeMinutes = estimatedTimeMinutes;
        edge.distanceMeters = distanceMeters;
        return edge;
    }

    public void updateRoute(Integer estimatedTimeMinutes, Integer distanceMeters) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.distanceMeters = distanceMeters;
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