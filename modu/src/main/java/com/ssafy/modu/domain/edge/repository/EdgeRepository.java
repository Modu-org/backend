package com.ssafy.modu.domain.edge.repository;

import com.ssafy.modu.domain.edge.entity.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EdgeRepository extends JpaRepository<Edge, Long> {

    boolean existsByScheduleIdAndFromNodeIdAndToNodeId(
            Long scheduleId,
            Long fromNodeId,
            Long toNodeId
    );

    Optional<Edge> findByScheduleIdAndFromNodeIdAndToNodeId(
            Long scheduleId,
            Long fromNodeId,
            Long toNodeId
    );

    List<Edge> findByScheduleId(Long scheduleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        delete from Edge e
        where e.schedule.id = :scheduleId
    """)
    void deleteAllByScheduleId(Long scheduleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Edge e
        where e.fromNode.id = :nodeId
           or e.toNode.id = :nodeId
    """)
    void deleteAllByNodeId(Long nodeId);
}