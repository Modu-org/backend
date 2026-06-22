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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * 2. start/end 조건 노드만 해당 날짜에 고정하고 나머지 노드는 전체 일정 기간에 다시 자동 배치
     * 3. 같은 날짜 노드들 사이 필요한 Edge를 준비한다. 기존 Edge가 있으면 재사용하고, 없으면 새로 생성한다.
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

        // start/end 조건 노드만 날짜에 고정하고, 나머지 노드는 기존 날짜를 무시한 뒤 다시 배정한다.
        List<Node> arrangedNodes = placeNodes(schedule, nodes, conditionMap);

        // 기존 Edge는 재사용하고, 없는 Edge만 날짜 단위로 일괄 생성한다.
        edgeService.createMissingEdgesForNewlyPlacedNodes(arrangedNodes);

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

        Set<Long> fixedNodeIds = new HashSet<>();

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

            validateFixedNodeNotDuplicated(fixedNodeIds, startNodeId);
            validateFixedNodeNotDuplicated(fixedNodeIds, endNodeId);
        }
    }

    /**
     * 같은 노드가 여러 날짜의 start/end 조건으로 중복 지정되면 안 된다.
     */
    private void validateFixedNodeNotDuplicated(
            Set<Long> fixedNodeIds,
            Long nodeId
    ) {
        if (nodeId == null) {
            return;
        }

        if (!fixedNodeIds.add(nodeId)) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }
    }

    /**
     * 자동배치 대상 노드를 스케줄 기간 안의 날짜에 재분배한다.
     *
     * 재배치 기준:
     * 1. startNodeId/endNodeId로 지정된 노드는 해당 날짜에 우선 고정한다.
     * 2. start/end 고정 노드가 하나도 없으면, 날짜별 대표 seed 노드를 먼저 배치한다.
     * 3. 고정되지 않은 나머지 노드는 기존 visitDate를 유지하지 않고 전체 일정 기간에 다시 배정한다.
     * 4. 특정 날짜에 너무 몰리지 않도록 최대 노드 수 기준을 둔다.
     */
    private List<Node> placeNodes(
            Schedule schedule,
            List<Node> nodes,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        List<Node> arrangedNodes = new ArrayList<>();

        List<LocalDate> scheduleDates = getScheduleDates(schedule);

        Map<Long, Node> nodeMap = nodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        Map<Long, LocalDate> fixedDateByNodeId = buildFixedDateByNodeId(conditionMap);

        /*
         * 자동배치 계산용 날짜별 노드 그룹.
         * 전체 재배치 정책이므로 기존 visitDate는 기준으로 사용하지 않는다.
         * start/end 고정 노드 또는 seed 노드를 먼저 배치한 뒤,
         * 나머지 노드를 가까운 날짜 그룹에 붙인다.
         */
        Map<LocalDate, List<Node>> plannedNodesByDate = new HashMap<>();

        for (LocalDate date : scheduleDates) {
            plannedNodesByDate.put(date, new ArrayList<>());
        }

        int maxNodesPerDay = calculateMaxNodesPerDay(
                nodes.size(),
                scheduleDates.size()
        );

        /*
         * 1. 시작/종료 노드 조건이 있는 노드를 기존 날짜와 관계없이 해당 날짜에 고정한다.
         */
        for (Map.Entry<Long, LocalDate> entry : fixedDateByNodeId.entrySet()) {
            placeFixedNode(
                    nodeMap,
                    plannedNodesByDate,
                    arrangedNodes,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        /*
         * 2. start/end 고정 노드가 하나도 없으면,
         *    날짜별 위치 기준점 역할을 할 seed 노드를 먼저 배치한다.
         */
        if (fixedDateByNodeId.isEmpty()) {
            placeSeedNodes(
                    scheduleDates,
                    plannedNodesByDate,
                    arrangedNodes,
                    nodes
            );
        }

        /*
         * 3. 아직 배치되지 않은 나머지 노드는 전체 자동 재배치 정책에 따라 다시 날짜를 배정한다.
         */
        Set<Long> arrangedNodeIds = arrangedNodes.stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        List<Node> movableNodes = nodes.stream()
                .filter(node -> !arrangedNodeIds.contains(node.getId()))
                .sorted(Comparator.comparing(Node::getId))
                .toList();

        for (Node node : movableNodes) {
            LocalDate targetDate = findBestDateForNode(
                    node,
                    scheduleDates,
                    plannedNodesByDate,
                    maxNodesPerDay
            );

            int nextVisitOrder = plannedNodesByDate.get(targetDate).size() + 1;

            node.updateVisitInfo(nextVisitOrder, targetDate);

            plannedNodesByDate.get(targetDate).add(node);
            arrangedNodes.add(node);
        }

        return arrangedNodes;
    }

    /**
     * 고정되지 않은 노드를 어느 날짜 그룹에 배치할지 결정한다.
     *
     * 기준:
     * - 해당 날짜에 이미 배치된 노드들과 가까울수록 좋다.
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
     * 특정 날짜에 node를 배치했을 때의 휴리스틱 점수를 계산한다.
     *
     * 점수가 낮을수록 더 좋은 날짜다.
     *
     * 기준:
     * - 해당 날짜에 이미 start/end 고정 노드나 seed 노드가 있으면, 그 그룹과의 거리를 기준으로 한다.
     * - 아직 아무 노드도 없는 날짜는 기본 점수를 부여한다.
     * - 날짜별 노드 수가 너무 많아지지 않도록 점진적 페널티를 부여한다.
     */
    private double calculateDateScore(
            Node node,
            LocalDate date,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            int maxNodesPerDay
    ) {
        List<Node> dateNodes = plannedNodesByDate.getOrDefault(date, List.of());

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

        /*
         * maxNodesPerDay는 절대 제한이 아니라 권장 기준으로 사용한다.
         * 가까운 노드끼리는 같은 날짜에 더 많이 묶일 수 있도록,
         * 권장 개수를 초과한 경우에도 점진적으로 페널티를 준다.
         */
        int overCount = Math.max(0, currentCount - maxNodesPerDay + 1);

        double crowdPenalty = currentCount * 5.0
                + overCount * overCount * 30.0;

        return distanceScore + crowdPenalty;
    }

    /**
     * targetNode가 특정 날짜 그룹과 얼마나 가까운지 계산한다.
     *
     * 날짜 그룹 안에 이미 배치된 노드들 중
     * targetNode와 가장 가까운 노드와의 직선거리를 기준으로 한다.
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
     * 날짜별 startNodeId/endNodeId 조건을 nodeId -> 고정 날짜 Map으로 변환한다.
     */
    private Map<Long, LocalDate> buildFixedDateByNodeId(
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        Map<Long, LocalDate> fixedDateByNodeId = new LinkedHashMap<>();

        for (AutoArrangeRequest.DayCondition condition : conditionMap.values()) {
            LocalDate targetDate = condition.getDate();

            addFixedDateIfPresent(fixedDateByNodeId, condition.getStartNodeId(), targetDate);
            addFixedDateIfPresent(fixedDateByNodeId, condition.getEndNodeId(), targetDate);
        }

        return fixedDateByNodeId;
    }

    /**
     * start/end 조건이 있는 노드를 고정 날짜 Map에 추가한다.
     */
    private void addFixedDateIfPresent(
            Map<Long, LocalDate> fixedDateByNodeId,
            Long nodeId,
            LocalDate targetDate
    ) {
        if (nodeId == null) {
            return;
        }

        fixedDateByNodeId.put(nodeId, targetDate);
    }

    /**
     * startNodeId/endNodeId로 지정된 노드를 기존 날짜와 관계없이 해당 날짜에 고정한다.
     */
    private void placeFixedNode(
            Map<Long, Node> nodeMap,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            List<Node> arrangedNodes,
            Long nodeId,
            LocalDate targetDate
    ) {
        Node node = nodeMap.get(nodeId);

        if (node == null) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
        }

        int nextVisitOrder = plannedNodesByDate.get(targetDate).size() + 1;

        node.updateVisitInfo(nextVisitOrder, targetDate);

        plannedNodesByDate.get(targetDate).add(node);
        arrangedNodes.add(node);
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
        validateNodeDateNotChanged(request, response);
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
     * AI는 서버가 확정한 node별 visitDate를 변경하면 안 된다.
     *
     * 서버가 날짜 배치를 담당하고,
     * AI는 같은 날짜 안에서 visitOrder만 추천한다.
     */
    private void validateNodeDateNotChanged(
            RouteRecommendAiRequest request,
            RouteRecommendAiResponse response
    ) {
        Map<Long, LocalDate> inputVisitDateByNodeId = request.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .collect(Collectors.toMap(
                        RouteRecommendAiRequest.NodeRequest::getNodeId,
                        RouteRecommendAiRequest.NodeRequest::getVisitDate
                ));

        for (RouteRecommendAiResponse.DayResponse day : response.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }

            for (RouteRecommendAiResponse.NodeResponse node : day.getNodes()) {
                LocalDate inputVisitDate = inputVisitDateByNodeId.get(node.getNodeId());

                if (!Objects.equals(inputVisitDate, node.getVisitDate())) {
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

    /**
     * start/end 고정 조건이 하나도 없을 때,
     * 각 날짜의 위치 기준점이 될 seed 노드를 먼저 배치한다.
     *
     * seed는 서로 최대한 떨어진 노드들로 선택한다.
     * 이렇게 하면 start/end가 없어도 날짜별로 지역 그룹이 나뉘는 효과를 얻을 수 있다.
     */
    private void placeSeedNodes(
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            List<Node> arrangedNodes,
            List<Node> nodes
    ) {
        int seedCount = Math.min(scheduleDates.size(), nodes.size());

        List<Node> seedNodes = selectSeedNodes(nodes, seedCount);

        for (int i = 0; i < seedNodes.size(); i++) {
            Node seedNode = seedNodes.get(i);
            LocalDate targetDate = scheduleDates.get(i);

            seedNode.updateVisitInfo(1, targetDate);

            plannedNodesByDate.get(targetDate).add(seedNode);
            arrangedNodes.add(seedNode);
        }
    }
    /**
     * 날짜별 seed 노드를 선택한다.
     *
     * 선택 기준:
     * 1. 좌표가 있는 노드를 우선 사용한다.
     * 2. 첫 seed는 id가 가장 작은 노드로 안정적으로 선택한다.
     * 3. 이후 seed는 기존 seed들과 가장 멀리 떨어진 노드를 선택한다.
     * 4. 좌표가 부족하면 id 순서로 부족한 seed를 채운다.
     */
    private List<Node> selectSeedNodes(
            List<Node> nodes,
            int seedCount
    ) {
        if (seedCount <= 0) {
            return List.of();
        }

        List<Node> coordinateNodes = nodes.stream()
                .filter(this::hasCoordinate)
                .sorted(Comparator.comparing(Node::getId))
                .toList();

        List<Node> seeds = new ArrayList<>();

        if (!coordinateNodes.isEmpty()) {
            seeds.add(coordinateNodes.get(0));

            while (seeds.size() < seedCount && seeds.size() < coordinateNodes.size()) {
                Node nextSeed = coordinateNodes.stream()
                        .filter(node -> !seeds.contains(node))
                        .max(Comparator
                                .comparingDouble((Node node) -> distanceToNearestSeed(node, seeds))
                                .thenComparing(Node::getId, Comparator.reverseOrder()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST));

                seeds.add(nextSeed);
            }
        }

        if (seeds.size() < seedCount) {
            Set<Long> seedIds = seeds.stream()
                    .map(Node::getId)
                    .collect(Collectors.toSet());

            List<Node> fallbackSeeds = nodes.stream()
                    .filter(node -> !seedIds.contains(node.getId()))
                    .sorted(Comparator.comparing(Node::getId))
                    .limit(seedCount - seeds.size())
                    .toList();

            seeds.addAll(fallbackSeeds);
        }

        return seeds;
    }
    /**
     * 노드가 위도/경도 좌표를 가지고 있는지 확인한다.
     */
    private boolean hasCoordinate(Node node) {
        return node.getAttraction() != null
                && node.getAttraction().getLatitude() != null
                && node.getAttraction().getLongitude() != null;
    }

    /**
     * node가 현재 seed 목록 중 가장 가까운 seed와 얼마나 떨어져 있는지 계산한다.
     */
    private double distanceToNearestSeed(
            Node node,
            List<Node> seeds
    ) {
        return seeds.stream()
                .mapToDouble(seed -> distanceKm(node, seed))
                .min()
                .orElse(Double.MAX_VALUE);
    }
}
