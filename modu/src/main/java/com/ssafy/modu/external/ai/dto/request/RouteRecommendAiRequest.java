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

        /*
         * AI에게 전달하는 시작 선호 노드.
         *
         * startFixed=true:
         * - 프론트 요청으로 들어온 강제 시작 노드
         * - 반드시 해당 날짜의 첫 번째 노드여야 한다.
         *
         * startFixed=false:
         * - 서버가 자동 배치 과정에서 추론한 추천 시작 노드
         * - 가능하면 첫 번째로 사용하되, 더 좋은 경로가 있으면 조정할 수 있다.
         */
        private Long preferredStartNodeId;

        /*
         * AI에게 전달하는 종료 선호 노드.
         *
         * endFixed=true:
         * - 프론트 요청으로 들어온 강제 종료 노드
         * - 반드시 해당 날짜의 마지막 노드여야 한다.
         *
         * endFixed=false:
         * - 서버가 자동 배치 과정에서 추론한 추천 종료 노드
         * - 가능하면 마지막으로 사용하되, 더 좋은 경로가 있으면 조정할 수 있다.
         */
        private Long preferredEndNodeId;

        /*
         * preferredStartNodeId가 프론트 요청으로 들어온 강제 조건인지 여부.
         */
        private boolean startFixed;

        /*
         * preferredEndNodeId가 프론트 요청으로 들어온 강제 조건인지 여부.
         */
        private boolean endFixed;

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