package com.ssafy.modu.domain.routerecommend.service;

import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.service.EdgeService;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.routerecommend.dto.request.AutoArrangeRequest;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.external.ai.AIService;
import com.ssafy.modu.external.ai.dto.request.RouteRecommendAiRequest;
import com.ssafy.modu.external.ai.dto.response.RouteRecommendAiResponse;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRecommendService {

    private final AIService aiService;
    private final ScheduleRepository scheduleRepository;
    private final EdgeService edgeService;

    /**
     * AI를 이용해서 일정 내 노드 방문 순서를 자동 추천한다.
     *
     * 흐름:
     * 1. 스케줄과 노드 조회
     * 2. 미배정 노드가 있으면 날짜에 자동 배치
     * 3. 같은 날짜 노드들 사이 Edge 생성
     * 4. AI 요청 생성
     * 5. AI 응답 검증
     * 6. 추천된 방문 순서 DB 반영
     */
    @Transactional
    public ScheduleDetailResponse autoArrange(
            Long scheduleId,
            Long userId,
            AutoArrangeRequest request
    ) {
        Schedule schedule = scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        List<Node> nodes = schedule.getNodes();

        Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap = toConditionMap(request);

        validateRequest(schedule, nodes, conditionMap);

        // 날짜가 없는 노드는 AI 요청 전에 날짜를 먼저 배정한다.
        List<Node> newlyPlacedNodes = placeUnscheduledNodes(schedule, nodes, conditionMap);

        // 새로 날짜가 배정된 노드 기준으로 Edge를 생성한다.
        for (Node node : newlyPlacedNodes) {
            edgeService.rebuildEdgesForMovedNode(node);
        }

        List<Edge> edges = edgeService.getEdgesByScheduleId(scheduleId);

        RouteRecommendAiRequest aiRequest = buildAiRequest(schedule, nodes, edges, conditionMap);

        RouteRecommendAiResponse aiResponse = aiService.recommendRoute(aiRequest);

        validateAiResponse(aiRequest, aiResponse, conditionMap);

        applyRecommendedOrder(nodes, aiResponse);

        List<Edge> finalEdges = edgeService.getEdgesByScheduleId(scheduleId);

        return ScheduleDetailResponse.from(schedule, finalEdges);
    }

    /**
     * 요청으로 들어온 날짜별 조건을 Map으로 변환한다.
     * key: 날짜
     * value: 해당 날짜의 시작/종료 노드 조건
     */
    private Map<LocalDate, AutoArrangeRequest.DayCondition> toConditionMap(
            AutoArrangeRequest request
    ) {
        if (request == null || request.getDays() == null) {
            return Map.of();
        }

        return request.getDays().stream()
                .filter(day -> day.getDate() != null)
                .collect(Collectors.toMap(
                        AutoArrangeRequest.DayCondition::getDate,
                        Function.identity(),
                        (existing, replacement) -> replacement,
                        TreeMap::new
                ));
    }

    /**
     * AI 추천 전에 기본 요청값을 검증한다.
     */
    private void validateRequest(
            Schedule schedule,
            List<Node> nodes,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        Set<Long> nodeIds = nodes.stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        for (AutoArrangeRequest.DayCondition condition : conditionMap.values()) {
            LocalDate date = condition.getDate();

            // 조건 날짜는 스케줄 기간 안에 있어야 한다.
            if (date.isBefore(schedule.getStartDate()) || date.isAfter(schedule.getEndDate())) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

            Long startNodeId = condition.getStartNodeId();
            Long endNodeId = condition.getEndNodeId();

            // 시작/종료 노드는 실제 스케줄에 포함된 노드여야 한다.
            if (startNodeId != null && !nodeIds.contains(startNodeId)) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

            if (endNodeId != null && !nodeIds.contains(endNodeId)) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

            // 시작 노드와 종료 노드가 같으면 안 된다.
            if (startNodeId != null && startNodeId.equals(endNodeId)) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }
        }
    }

    /**
     * 아직 날짜가 없는 노드를 스케줄 기간 안의 날짜에 자동 배치한다.
     *
     * 배치 기준:
     * 1. startNodeId/endNodeId로 지정된 노드는 해당 날짜에 우선 배치
     * 2. 나머지 미배정 노드는 가까운 날짜 그룹에 배치
     * 3. 특정 날짜에 너무 몰리지 않도록 최대 노드 수 기준을 둔다
     */
    private List<Node> placeUnscheduledNodes(
            Schedule schedule,
            List<Node> nodes,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        List<Node> placedNodes = new ArrayList<>();

        List<LocalDate> scheduleDates = getScheduleDates(schedule);

        Map<Long, Node> nodeMap = nodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        /*
         * 날짜별 노드 목록.
         * 기존에 visitDate가 있는 노드를 먼저 넣어둔다.
         */
        Map<LocalDate, List<Node>> nodesByDate = new HashMap<>();

        for (LocalDate date : scheduleDates) {
            nodesByDate.put(date, new ArrayList<>());
        }

        for (Node node : nodes) {
            if (node.getVisitDate() != null) {
                nodesByDate
                        .computeIfAbsent(node.getVisitDate(), key -> new ArrayList<>())
                        .add(node);
            }
        }

        int maxNodesPerDay = calculateMaxNodesPerDay(
                nodes.size(),
                scheduleDates.size()
        );

        /*
         * 1. 시작/종료 노드 조건이 있는 미배정 노드를 먼저 해당 날짜에 배치한다.
         */
        for (AutoArrangeRequest.DayCondition condition : conditionMap.values()) {
            LocalDate targetDate = condition.getDate();

            placeFixedNodeIfNeeded(
                    nodeMap,
                    nodesByDate,
                    placedNodes,
                    condition.getStartNodeId(),
                    targetDate
            );

            placeFixedNodeIfNeeded(
                    nodeMap,
                    nodesByDate,
                    placedNodes,
                    condition.getEndNodeId(),
                    targetDate
            );
        }

        /*
         * 2. 나머지 미배정 노드는 가까운 날짜 그룹에 배치한다.
         */
        List<Node> unscheduledNodes = nodes.stream()
                .filter(node -> node.getVisitDate() == null)
                .sorted(Comparator.comparing(Node::getId))
                .toList();

        for (Node node : unscheduledNodes) {
            LocalDate targetDate = findBestDateForNode(
                    node,
                    scheduleDates,
                    nodesByDate,
                    maxNodesPerDay
            );

            int nextVisitOrder = nodesByDate.get(targetDate).size() + 1;

            node.updateVisitInfo(nextVisitOrder, targetDate);

            nodesByDate.get(targetDate).add(node);
            placedNodes.add(node);
        }

        return placedNodes;
    }
    /**
     * 미배정 노드를 어느 날짜에 배치할지 결정한다.
     *
     * 기준:
     * - 해당 날짜에 이미 있는 노드들과 가까울수록 좋다.
     * - 특정 날짜에 노드가 너무 많으면 강한 페널티를 준다.
     * - 점수가 같으면 현재 노드 수가 적은 날짜를 우선한다.
     * - 그래도 같으면 빠른 날짜를 우선한다.
     */
    private LocalDate findBestDateForNode(
            Node node,
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> nodesByDate,
            int maxNodesPerDay
    ) {
        return scheduleDates.stream()
                .min(Comparator
                        .comparing((LocalDate date) -> calculateDateScore(
                                node,
                                date,
                                nodesByDate,
                                maxNodesPerDay
                        ))
                        .thenComparing(date -> nodesByDate.getOrDefault(date, List.of()).size())
                        .thenComparing(date -> date))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST));
    }

    /**
     * 특정 날짜에 node를 배치했을 때의 점수를 계산한다.
     * 점수가 낮을수록 더 좋은 날짜다.
     */
    private double calculateDateScore(
            Node node,
            LocalDate date,
            Map<LocalDate, List<Node>> nodesByDate,
            int maxNodesPerDay
    ) {
        List<Node> dateNodes = nodesByDate.getOrDefault(date, List.of());

        double distanceScore;

        if (dateNodes.isEmpty()) {
            /*
             * 아직 아무 노드도 없는 날짜는 거리 비교 기준이 없다.
             * 너무 유리하거나 불리하지 않게 기본 점수를 준다.
             */
            distanceScore = 50.0;
        } else {
            distanceScore = distanceToDateGroup(node, dateNodes);
        }

        int currentCount = dateNodes.size();

        double crowdPenalty;

        if (currentCount >= maxNodesPerDay) {
            /*
             * 하루 최대 권장 노드 수를 넘으면 강한 페널티.
             */
            crowdPenalty = 10000.0;
        } else {
            /*
             * 노드가 많을수록 약간의 페널티.
             * 가까운 날짜를 우선하되, 너무 몰리지 않게 한다.
             */
            crowdPenalty = currentCount * 5.0;
        }

        return distanceScore + crowdPenalty;
    }

    /**
     * targetNode가 특정 날짜 그룹과 얼마나 가까운지 계산한다.
     * 날짜 그룹 안의 노드들 중 가장 가까운 거리 기준.
     */
    private double distanceToDateGroup(
            Node targetNode,
            List<Node> dateNodes
    ) {
        if (dateNodes == null || dateNodes.isEmpty()) {
            return Double.MAX_VALUE;
        }

        return dateNodes.stream()
                .mapToDouble(node -> distanceKm(targetNode, node))
                .min()
                .orElse(Double.MAX_VALUE);
    }

    /**
     * 두 노드 사이의 위도/경도 기반 직선거리(km)를 계산한다.
     */
    private double distanceKm(Node a, Node b) {
        if (a.getAttraction() == null || b.getAttraction() == null) {
            return Double.MAX_VALUE;
        }

        if (a.getAttraction().getLatitude() == null
                || a.getAttraction().getLongitude() == null
                || b.getAttraction().getLatitude() == null
                || b.getAttraction().getLongitude() == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = a.getAttraction().getLatitude().doubleValue();
        double lon1 = a.getAttraction().getLongitude().doubleValue();
        double lat2 = b.getAttraction().getLatitude().doubleValue();
        double lon2 = b.getAttraction().getLongitude().doubleValue();

        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));

        return earthRadiusKm * c;
    }

    /**
     * 날짜별 최대 권장 노드 수를 계산한다.
     * 예: 전체 노드 9개, 일정 2일이면 하루 최대 5개.
     */
    private int calculateMaxNodesPerDay(
            int totalNodeCount,
            int dayCount
    ) {
        if (dayCount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        return (int) Math.ceil(totalNodeCount / (double) dayCount);
    }

    /**
     * startNodeId/endNodeId로 지정된 노드가 미배정 상태라면 해당 날짜에 배치한다.
     */
    private void placeFixedNodeIfNeeded(
            Map<Long, Node> nodeMap,
            Map<LocalDate, List<Node>> nodesByDate,
            List<Node> placedNodes,
            Long nodeId,
            LocalDate targetDate
    ) {
        if (nodeId == null) {
            return;
        }

        Node node = nodeMap.get(nodeId);

        if (node == null) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        // 이미 다른 날짜에 배치된 노드를 강제로 옮기지는 않는다.
        if (node.getVisitDate() != null && !node.getVisitDate().equals(targetDate)) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        if (node.getVisitDate() == null) {
            int nextVisitOrder = nodesByDate.get(targetDate).size() + 1;

            node.updateVisitInfo(nextVisitOrder, targetDate);

            nodesByDate.get(targetDate).add(node);
            placedNodes.add(node);
        }
    }

    /**
     * 스케줄 시작일 ~ 종료일까지의 날짜 목록을 만든다.
     */
    private List<LocalDate> getScheduleDates(Schedule schedule) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate current = schedule.getStartDate();

        while (!current.isAfter(schedule.getEndDate())) {
            dates.add(current);
            current = current.plusDays(1);
        }

        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        return dates;
    }

    /**
     * AI 서버에 보낼 요청 객체를 만든다.
     */
    private RouteRecommendAiRequest buildAiRequest(
            Schedule schedule,
            List<Node> nodes,
            List<Edge> edges,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        Map<Integer, String> contentTypeMap = Map.of(
                12, "관광지",
                14, "문화시설",
                15, "축제공연행사",
                25, "여행코스",
                28, "레포츠",
                32, "숙박",
                38, "쇼핑",
                39, "음식점"
        );

        RouteRecommendAiRequest.Constraints constraints =
                new RouteRecommendAiRequest.Constraints(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true
                );

        List<RouteRecommendAiRequest.DayRequest> dayRequests =
                buildDayRequests(schedule, nodes, edges, conditionMap);

        return new RouteRecommendAiRequest(
                schedule.getId(),
                "이동 시간을 최소화하면서 관광지 타입이 골고루 섞이도록 여행 경로를 추천",
                contentTypeMap,
                constraints,
                dayRequests
        );
    }

    /**
     * 날짜별 노드와 Edge 정보를 AI 요청 형식으로 변환한다.
     */
    private List<RouteRecommendAiRequest.DayRequest> buildDayRequests(
            Schedule schedule,
            List<Node> nodes,
            List<Edge> edges,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        Map<LocalDate, List<Node>> nodesByDate = nodes.stream()
                .filter(node -> node.getVisitDate() != null)
                .collect(Collectors.groupingBy(
                        Node::getVisitDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<RouteRecommendAiRequest.DayRequest> dayRequests = new ArrayList<>();

        LocalDate current = schedule.getStartDate();

        while (!current.isAfter(schedule.getEndDate())) {
            AutoArrangeRequest.DayCondition condition = conditionMap.get(current);

            Long startNodeId = condition != null ? condition.getStartNodeId() : null;
            Long endNodeId = condition != null ? condition.getEndNodeId() : null;

            List<Node> dayNodes = nodesByDate.getOrDefault(current, List.of()).stream()
                    .sorted(Comparator
                            .comparing(Node::getVisitOrder)
                            .thenComparing(Node::getId))
                    .toList();

            Set<Long> dayNodeIds = dayNodes.stream()
                    .map(Node::getId)
                    .collect(Collectors.toSet());

            List<RouteRecommendAiRequest.NodeRequest> nodeRequests = dayNodes.stream()
                    .map(node -> new RouteRecommendAiRequest.NodeRequest(
                            node.getId(),
                            schedule.getId(),
                            node.getAttraction() != null ? node.getAttraction().getId() : null,
                            node.getVisitOrder(),
                            node.getVisitDate(),
                            node.getAttraction() != null
                                    ? parseContentTypeId(node.getAttraction().getContentTypeId())
                                    : null
                    ))
                    .toList();

            List<RouteRecommendAiRequest.EdgeRequest> edgeRequests = edges.stream()
                    .filter(edge ->
                            dayNodeIds.contains(edge.getFromNode().getId())
                                    && dayNodeIds.contains(edge.getToNode().getId()))
                    .map(edge -> new RouteRecommendAiRequest.EdgeRequest(
                            edge.getId(),
                            edge.getFromNode().getId(),
                            edge.getToNode().getId(),
                            edge.getEstimatedTimeMinutes(),
                            edge.getDistanceMeters()
                    ))
                    .toList();

            dayRequests.add(new RouteRecommendAiRequest.DayRequest(
                    current,
                    startNodeId,
                    endNodeId,
                    nodeRequests,
                    edgeRequests
            ));

            current = current.plusDays(1);
        }

        return dayRequests;
    }

    /**
     * AI 응답이 서버가 보낸 요청 조건을 지켰는지 검증한다.
     */
    private void validateAiResponse(
            RouteRecommendAiRequest request,
            RouteRecommendAiResponse response,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        if (response == null || response.getDays() == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
        }

        Set<Long> inputNodeIds = request.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .map(RouteRecommendAiRequest.NodeRequest::getNodeId)
                .collect(Collectors.toSet());

        List<RouteRecommendAiResponse.NodeResponse> outputNodes = response.getDays().stream()
                .filter(day -> day.getNodes() != null)
                .flatMap(day -> day.getNodes().stream())
                .toList();

        Set<Long> outputNodeIds = outputNodes.stream()
                .map(RouteRecommendAiResponse.NodeResponse::getNodeId)
                .collect(Collectors.toSet());

        // AI가 노드를 누락하거나 없는 노드를 추가하면 안 된다.
        if (!inputNodeIds.equals(outputNodeIds)) {
            throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
        }

        validateNodeDates(response);
        validateVisitOrders(response);
        validateDayConditions(response, conditionMap);
        rebuildEdgesInResponse(request, response);
    }

    /**
     * AI 응답의 node.visitDate와 day.date가 일치하는지 확인한다.
     */
    private void validateNodeDates(RouteRecommendAiResponse response) {
        for (RouteRecommendAiResponse.DayResponse day : response.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (RouteRecommendAiResponse.NodeResponse node : day.getNodes()) {
                if (!Objects.equals(day.getDate(), node.getVisitDate())) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }
            }
        }
    }

    /**
     * 날짜별 visitOrder가 1부터 연속인지 확인한다.
     */
    private void validateVisitOrders(RouteRecommendAiResponse response) {
        for (RouteRecommendAiResponse.DayResponse day : response.getDays()) {
            List<RouteRecommendAiResponse.NodeResponse> nodes =
                    day.getNodes() != null ? day.getNodes() : List.of();

            List<Integer> orders = nodes.stream()
                    .map(RouteRecommendAiResponse.NodeResponse::getVisitOrder)
                    .sorted()
                    .toList();

            for (int i = 0; i < orders.size(); i++) {
                if (!Objects.equals(orders.get(i), i + 1)) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }
            }
        }
    }

    /**
     * startNodeId/endNodeId 조건을 AI가 지켰는지 확인한다.
     */
    private void validateDayConditions(
            RouteRecommendAiResponse response,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        Map<LocalDate, List<RouteRecommendAiResponse.NodeResponse>> nodesByDate =
                response.getDays().stream()
                        .collect(Collectors.toMap(
                                RouteRecommendAiResponse.DayResponse::getDate,
                                day -> day.getNodes() != null
                                        ? day.getNodes().stream()
                                        .sorted(Comparator.comparing(RouteRecommendAiResponse.NodeResponse::getVisitOrder))
                                        .toList()
                                        : List.of(),
                                (existing, replacement) -> replacement
                        ));

        for (AutoArrangeRequest.DayCondition condition : conditionMap.values()) {
            LocalDate date = condition.getDate();

            List<RouteRecommendAiResponse.NodeResponse> dayNodes =
                    nodesByDate.getOrDefault(date, List.of());

            if (condition.getStartNodeId() != null) {
                if (dayNodes.isEmpty()
                        || !condition.getStartNodeId().equals(dayNodes.get(0).getNodeId())) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }
            }

            if (condition.getEndNodeId() != null) {
                if (dayNodes.isEmpty()
                        || !condition.getEndNodeId().equals(dayNodes.get(dayNodes.size() - 1).getNodeId())) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }
            }
        }
    }

    /**
     * AI 응답에 들어온 Edge를 그대로 믿지 않고,
     * 서버가 가진 Edge 정보로 다시 구성한다.
     */
    private void rebuildEdgesInResponse(
            RouteRecommendAiRequest request,
            RouteRecommendAiResponse response
    ) {
        Map<String, RouteRecommendAiRequest.EdgeRequest> inputEdgeMap = request.getDays().stream()
                .flatMap(day -> day.getEdges().stream())
                .collect(Collectors.toMap(
                        edge -> edgeKey(edge.getFromNodeId(), edge.getToNodeId()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        for (RouteRecommendAiResponse.DayResponse day : response.getDays()) {
            List<RouteRecommendAiResponse.NodeResponse> sortedNodes =
                    day.getNodes() != null
                            ? day.getNodes().stream()
                            .sorted(Comparator.comparing(RouteRecommendAiResponse.NodeResponse::getVisitOrder))
                            .toList()
                            : List.of();

            List<RouteRecommendAiResponse.EdgeResponse> rebuiltEdges = new ArrayList<>();

            int totalMinutes = 0;
            int totalDistance = 0;

            for (int i = 0; i < sortedNodes.size() - 1; i++) {
                Long fromNodeId = sortedNodes.get(i).getNodeId();
                Long toNodeId = sortedNodes.get(i + 1).getNodeId();

                RouteRecommendAiRequest.EdgeRequest inputEdge =
                        inputEdgeMap.get(edgeKey(fromNodeId, toNodeId));

                if (inputEdge == null) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }

                RouteRecommendAiResponse.EdgeResponse edgeResponse =
                        new RouteRecommendAiResponse.EdgeResponse();

                edgeResponse.setEdgeId(inputEdge.getEdgeId());
                edgeResponse.setFromNodeId(inputEdge.getFromNodeId());
                edgeResponse.setToNodeId(inputEdge.getToNodeId());
                edgeResponse.setEstimatedTimeMinutes(inputEdge.getEstimatedTimeMinutes());
                edgeResponse.setDistanceMeters(inputEdge.getDistanceMeters());

                rebuiltEdges.add(edgeResponse);

                totalMinutes += inputEdge.getEstimatedTimeMinutes();
                totalDistance += inputEdge.getDistanceMeters();
            }

            day.setEdges(rebuiltEdges);

            if (day.getSummary() == null) {
                day.setSummary(new RouteRecommendAiResponse.Summary());
            }

            day.getSummary().setTotalEstimatedTimeMinutes(totalMinutes);
            day.getSummary().setTotalDistanceMeters(totalDistance);
        }
    }

    /**
     * AI 추천 결과를 실제 Node 엔티티에 반영한다.
     */
    private void applyRecommendedOrder(
            List<Node> originalNodes,
            RouteRecommendAiResponse response
    ) {
        Map<Long, Node> nodeMap = originalNodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        for (RouteRecommendAiResponse.DayResponse day : response.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (RouteRecommendAiResponse.NodeResponse recommendedNode : day.getNodes()) {
                Node node = nodeMap.get(recommendedNode.getNodeId());

                if (node == null) {
                    throw new BusinessException(ErrorCode.INVALID_AI_RECOMMENDATION);
                }

                node.updateVisitInfo(recommendedNode.getVisitOrder(), day.getDate());
            }
        }
    }

    private Integer parseContentTypeId(String contentTypeId) {
        if (contentTypeId == null || contentTypeId.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(contentTypeId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String edgeKey(Long fromNodeId, Long toNodeId) {
        return fromNodeId + "-" + toNodeId;
    }
}