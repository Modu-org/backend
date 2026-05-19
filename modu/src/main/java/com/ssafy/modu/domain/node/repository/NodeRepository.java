package com.ssafy.modu.domain.node.repository;

import com.ssafy.modu.domain.node.entity.Node;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByIdAndSchedule_Id(Long nodeId, Long scheduleId);

    // 노드기반으로 관광지 정보 + 무장애 정보 조회
    @EntityGraph(attributePaths = {"attraction", "attraction.accessibilityInfos"})
    Optional<Node> findWithAttractionAccessibilityByIdAndSchedule_Id(Long nodeId, Long scheduleId);

    // 스케줄의 노드 개수 세기
    long countBySchedule_Id(Long scheduleId);

    // 방문 일자, 방문 순서 기준으로 정렬
    List<Node> findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(
            Long scheduleId,
            LocalDate visitDate
    );
}
