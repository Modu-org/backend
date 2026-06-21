package com.ssafy.modu.domain.arrival.repository;

import com.ssafy.modu.domain.arrival.entity.ArrivalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArrivalLogRepository extends JpaRepository<ArrivalLog, Long> {
    List<ArrivalLog> findAllByScheduleIdOrderByRequestedAtDesc(Long scheduleId);
    Optional<ArrivalLog> findFirstByScheduleIdAndNodeIdAndArrivedTrueOrderByRequestedAtDesc(
            Long scheduleId,
            Long nodeId
    );

}