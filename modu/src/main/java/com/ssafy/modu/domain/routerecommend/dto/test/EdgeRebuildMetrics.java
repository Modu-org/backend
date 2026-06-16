package com.ssafy.modu.domain.routerecommend.dto.test;

import java.time.LocalDate;

public record EdgeRebuildMetrics(
        Long nodeId,
        LocalDate visitDate,
        int sameDateNodeCount,
        int edgeCandidateCount,
        int existingEdgeCount,
        int createdEdgeCount,
        int kakaoApiCallCount,
        long elapsedMs
) {
    public static EdgeRebuildMetrics skipped(Long nodeId, LocalDate visitDate, long elapsedMs) {
        return new EdgeRebuildMetrics(
                nodeId,
                visitDate,
                0,
                0,
                0,
                0,
                0,
                elapsedMs
        );
    }
}