package com.ssafy.modu.domain.edge.dto.response;

import com.ssafy.modu.domain.edge.entity.Edge;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EdgeResponse {

    private Long edgeId;
    private Long fromNodeId;
    private Long toNodeId;
    private Integer estimatedTimeMinutes;
    private Integer distanceMeters;

    public static EdgeResponse from(Edge edge) {
        return EdgeResponse.builder()
                .edgeId(edge.getId())
                .fromNodeId(edge.getFromNode().getId())
                .toNodeId(edge.getToNode().getId())
                .estimatedTimeMinutes(edge.getEstimatedTimeMinutes())
                .distanceMeters(edge.getDistanceMeters())
                .build();
    }
}