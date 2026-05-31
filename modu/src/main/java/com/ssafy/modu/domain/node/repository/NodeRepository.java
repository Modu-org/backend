package com.ssafy.modu.domain.node.repository;

import com.ssafy.modu.domain.node.entity.Node;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByIdAndSchedule_Id(Long nodeId, Long scheduleId);

    @EntityGraph(attributePaths = {"attraction", "attraction.accessibilityInfos"})
    Optional<Node> findWithAttractionAccessibilityByIdAndSchedule_Id(Long nodeId, Long scheduleId);

    long countBySchedule_Id(Long scheduleId);

    List<Node> findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(
            Long scheduleId,
            LocalDate visitDate
    );

    @EntityGraph(attributePaths = {"attraction"})
    List<Node> findWithAttractionBySchedule_IdAndVisitDate(
            Long scheduleId,
            LocalDate visitDate
    );
}