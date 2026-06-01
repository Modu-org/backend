package com.ssafy.modu.external.ai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RouteRecommendAiResponse {

    private List<DayResponse> days;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DayResponse {

        private LocalDate date;
        private List<NodeResponse> nodes;
        private List<EdgeResponse> edges;
        private Summary summary;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class NodeResponse {

        private Long nodeId;
        private Long scheduleId;
        private Long attractionId;
        private Integer visitOrder;
        private LocalDate visitDate;
        private Integer contentTypeId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EdgeResponse {

        private Long edgeId;
        private Long fromNodeId;
        private Long toNodeId;
        private Integer estimatedTimeMinutes;
        private Integer distanceMeters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Summary {

        private Integer totalEstimatedTimeMinutes;
        private Integer totalDistanceMeters;
        private String contentTypeBalanceComment;
    }
}