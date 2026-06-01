package com.ssafy.modu.external.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class RouteRecommendAiRequest {

    private Long scheduleId;
    private String goal;
    private Map<Integer, String> contentTypeMap;
    private Constraints constraints;
    private List<DayRequest> days;

    @Getter
    @AllArgsConstructor
    public static class Constraints {
        private boolean mustUseAllNodes;
        private boolean doNotCreateNewNode;
        private boolean doNotCreateNewEdge;
        private boolean useOnlyGivenEdges;
        private boolean avoidSameContentTypeSequence;
        private boolean minimizeTotalEstimatedTimeMinutes;
    }

    @Getter
    @AllArgsConstructor
    public static class DayRequest {

        private LocalDate date;

        // 해당 날짜에서 첫 번째로 고정할 nodeId
        private Long startNodeId;

        // 해당 날짜에서 마지막으로 고정할 nodeId
        private Long endNodeId;

        // 해당 날짜에 배치된 노드들
        private List<NodeRequest> nodes;

        // 해당 날짜 노드들 사이의 edge 후보들
        private List<EdgeRequest> edges;
    }

    @Getter
    @AllArgsConstructor
    public static class NodeRequest {

        private Long nodeId;
        private Long scheduleId;
        private Long attractionId;
        private Integer visitOrder;
        private LocalDate visitDate;
        private Integer contentTypeId;
    }

    @Getter
    @AllArgsConstructor
    public static class EdgeRequest {

        private Long edgeId;
        private Long fromNodeId;
        private Long toNodeId;
        private Integer estimatedTimeMinutes;
        private Integer distanceMeters;
    }
}