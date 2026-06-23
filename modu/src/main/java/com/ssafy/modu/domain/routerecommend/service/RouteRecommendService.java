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
     * 2. 사용자가 지정한 start/end는 fixed boundary로 처리한다.
     * 3. 날짜 수에 맞게 seed 기반 군집 배치를 수행한다.
     * 4. 각 날짜의 inferred start/end를 계산한다.
     * 5. Day N의 end가 Day N+1의 start 계산에 반영되도록 한다.
     * 6. AI 요청에는 fixed + inferred를 합친 effective boundary를 전달한다.
     * 7. AI 응답은 서버가 검증한 뒤 DB에 반영한다.
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

        PlanningResult planningResult = placeNodes(schedule, nodes, conditionMap);

        edgeService.createMissingEdgesForNewlyPlacedNodes(planningResult.arrangedNodes());

        List<Edge> edges = edgeService.getEdgesByScheduleId(scheduleId);

        RouteRecommendAiRequest aiRequest = buildAiRequest(
                schedule,
                nodes,
                edges,
                planningResult.boundaryMap()
        );

        RouteRecommendAiResponse aiResponse = aiService.recommendRoute(aiRequest);

        /*
         * conditionMap은 사용자가 명시한 fixed start/end 조건만 가진다.
         * inferred boundary는 AI 요청에는 전달되지만, 서버 검증에서는 강제하지 않는다.
         */
        validateAiResponse(aiRequest, aiResponse, conditionMap);

        applyRecommendedOrder(nodes, aiResponse);

        List<Edge> finalEdges = edgeService.getEdgesByScheduleId(scheduleId);

        return ScheduleDetailResponse.from(schedule, finalEdges);
    }

    /**
     * 날짜 배치 결과와 날짜별 boundary 정보를 함께 담는다.
     */
    private record PlanningResult(
            List<Node> arrangedNodes,
            Map<LocalDate, DayBoundary> boundaryMap
    ) {
    }

    /**
     * fixed boundary:
     * - 사용자가 직접 지정한 start/end
     * - 반드시 지켜야 하는 조건
     *
     * inferred boundary:
     * - 서버가 자동 배치 과정에서 추론한 start/end
     * - 날짜 간 연결성을 위해 AI에게 전달하는 참고 조건
     */
    private record DayBoundary(
            LocalDate date,
            Long fixedStartNodeId,
            Long fixedEndNodeId,
            Long inferredStartNodeId,
            Long inferredEndNodeId
    ) {
        Long effectiveStartNodeId() {
            return fixedStartNodeId != null ? fixedStartNodeId : inferredStartNodeId;
        }

        Long effectiveEndNodeId() {
            return fixedEndNodeId != null ? fixedEndNodeId : inferredEndNodeId;
        }
    }

    /**
     * 요청으로 들어온 날짜별 조건을 Map으로 변환한다.
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

            if (date.isBefore(schedule.getStartDate()) || date.isAfter(schedule.getEndDate())) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

            Long startNodeId = condition.getStartNodeId();
            Long endNodeId = condition.getEndNodeId();

            if (startNodeId != null && !nodeIds.contains(startNodeId)) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

            if (endNodeId != null && !nodeIds.contains(endNodeId)) {
                throw new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST);
            }

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
     * 자동배치 대상 노드를 날짜별로 배치한다.
     *
     * 핵심:
     * - fixed start/end는 해당 날짜에 먼저 배치한다.
     * - 빈 날짜는 seed로 먼저 채운다.
     * - 나머지 노드는 군집 점수 + 날짜 간 연결 점수로 배치한다.
     * - 각 날짜의 inferred start/end를 계산한다.
     */
    private PlanningResult placeNodes(
            Schedule schedule,
            List<Node> nodes,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        List<LocalDate> scheduleDates = getScheduleDates(schedule);

        Map<Long, Node> nodeMap = nodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        Map<Long, LocalDate> fixedDateByNodeId = buildFixedDateByNodeId(conditionMap);

        Map<LocalDate, List<Node>> plannedNodesByDate = new TreeMap<>();

        for (LocalDate date : scheduleDates) {
            plannedNodesByDate.put(date, new ArrayList<>());
        }

        List<Node> arrangedNodes = new ArrayList<>();

        /*
         * 1. 사용자가 지정한 start/end 노드는 해당 날짜에 먼저 고정 배치한다.
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
         * 2. 각 날짜가 가능하면 비지 않도록 seed를 먼저 배치한다.
         *    단, 이미 fixed 노드가 들어간 날짜는 seed를 추가하지 않는다.
         */
        placeSeedNodesForEmptyDates(
                scheduleDates,
                plannedNodesByDate,
                arrangedNodes,
                nodes
        );

        Set<Long> arrangedNodeIds = arrangedNodes.stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        List<Node> movableNodes = nodes.stream()
                .filter(node -> !arrangedNodeIds.contains(node.getId()))
                .sorted(Comparator.comparing(Node::getId))
                .toList();

        int maxNodesPerDay = calculateMaxNodesPerDay(
                nodes.size(),
                scheduleDates.size()
        );

        Map<LocalDate, DayBoundary> boundaryMap = inferBoundaries(
                scheduleDates,
                plannedNodesByDate,
                conditionMap
        );

        /*
         * 3. 나머지 노드는 군집 점수 + 날짜 간 연결 점수로 배치한다.
         *    노드가 배치될 때마다 boundary를 다시 계산해서,
         *    자동으로 생긴 Day N end가 Day N+1 start 판단에 반영되도록 한다.
         */
        for (Node node : movableNodes) {
            LocalDate targetDate = findBestDateForNode(
                    node,
                    scheduleDates,
                    plannedNodesByDate,
                    boundaryMap,
                    nodeMap,
                    maxNodesPerDay
            );

            plannedNodesByDate.get(targetDate).add(node);
            arrangedNodes.add(node);

            boundaryMap = inferBoundaries(
                    scheduleDates,
                    plannedNodesByDate,
                    conditionMap
            );
        }

        /*
         * 4. 최종 날짜 그룹 기준으로 inferred start/end를 계산하고,
         *    그 boundary에 맞춰 임시 visitOrder를 부여한다.
         */
        boundaryMap = inferBoundaries(
                scheduleDates,
                plannedNodesByDate,
                conditionMap
        );

        applyInitialOrdersByBoundary(
                scheduleDates,
                plannedNodesByDate,
                boundaryMap
        );

        return new PlanningResult(arrangedNodes, boundaryMap);
    }

    /**
     * 빈 날짜에 seed 노드를 배치한다.
     * 노드 수가 날짜 수보다 적으면 모든 날짜를 채울 수 없으므로 가능한 만큼만 배치한다.
     */
    private void placeSeedNodesForEmptyDates(
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            List<Node> arrangedNodes,
            List<Node> nodes
    ) {
        Set<Long> alreadyPlacedNodeIds = arrangedNodes.stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        List<LocalDate> emptyDates = scheduleDates.stream()
                .filter(date -> plannedNodesByDate.getOrDefault(date, List.of()).isEmpty())
                .toList();

        if (emptyDates.isEmpty()) {
            return;
        }

        List<Node> candidateNodes = nodes.stream()
                .filter(node -> !alreadyPlacedNodeIds.contains(node.getId()))
                .toList();

        if (candidateNodes.isEmpty()) {
            return;
        }

        int seedCount = Math.min(emptyDates.size(), candidateNodes.size());

        List<Node> seedNodes = selectSeedNodes(candidateNodes, seedCount);

        for (int i = 0; i < seedNodes.size(); i++) {
            Node seedNode = seedNodes.get(i);
            LocalDate targetDate = emptyDates.get(i);

            plannedNodesByDate.get(targetDate).add(seedNode);
            arrangedNodes.add(seedNode);
        }
    }

    /**
     * 고정되지 않은 노드를 어느 날짜 그룹에 배치할지 결정한다.
     */
    private LocalDate findBestDateForNode(
            Node node,
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            Map<LocalDate, DayBoundary> boundaryMap,
            Map<Long, Node> nodeMap,
            int maxNodesPerDay
    ) {
        return scheduleDates.stream()
                .min(Comparator
                        .comparing((LocalDate date) -> calculateDateScore(
                                node,
                                date,
                                scheduleDates,
                                plannedNodesByDate,
                                boundaryMap,
                                nodeMap,
                                maxNodesPerDay
                        ))
                        .thenComparing(date -> plannedNodesByDate.getOrDefault(date, List.of()).size())
                        .thenComparing(date -> date))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ROUTE_RECOMMENDATION_REQUEST));
    }

    /**
     * 특정 날짜에 node를 배치했을 때의 점수를 계산한다.
     *
     * 점수가 낮을수록 더 좋은 날짜다.
     *
     * 반영 요소:
     * - 해당 날짜 그룹과의 거리
     * - 이전 날짜 end와의 연결성
     * - 다음 날짜 start와의 연결성
     * - 약한 불균형 페널티
     */
    private double calculateDateScore(
            Node node,
            LocalDate date,
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            Map<LocalDate, DayBoundary> boundaryMap,
            Map<Long, Node> nodeMap,
            int maxNodesPerDay
    ) {
        List<Node> dateNodes = plannedNodesByDate.getOrDefault(date, List.of());

        double clusterScore = dateNodes.isEmpty()
                ? 0.0
                : distanceToDateGroup(node, dateNodes);

        double previousFlowScore = calculatePreviousFlowScore(
                node,
                date,
                scheduleDates,
                boundaryMap,
                nodeMap
        );

        double nextFlowScore = calculateNextFlowScore(
                node,
                date,
                scheduleDates,
                boundaryMap,
                nodeMap
        );

        double balancePenalty = calculateWeakBalancePenalty(
                date,
                plannedNodesByDate,
                maxNodesPerDay
        );

        return clusterScore
                + previousFlowScore * 0.4
                + nextFlowScore * 0.25
                + balancePenalty;
    }

    /**
     * 현재 node를 특정 날짜에 배치했을 때,
     * 이전 날짜의 end와 얼마나 자연스럽게 이어지는지 계산한다.
     */
    private double calculatePreviousFlowScore(
            Node node,
            LocalDate date,
            List<LocalDate> scheduleDates,
            Map<LocalDate, DayBoundary> boundaryMap,
            Map<Long, Node> nodeMap
    ) {
        LocalDate previousDate = getPreviousDate(scheduleDates, date);

        if (previousDate == null) {
            return 0.0;
        }

        DayBoundary previousBoundary = boundaryMap.get(previousDate);

        if (previousBoundary == null || previousBoundary.effectiveEndNodeId() == null) {
            return 0.0;
        }

        Node previousEndNode = nodeMap.get(previousBoundary.effectiveEndNodeId());

        if (previousEndNode == null) {
            return 0.0;
        }

        return distanceKm(previousEndNode, node);
    }

    /**
     * 현재 node를 특정 날짜에 배치했을 때,
     * 다음 날짜의 start와 얼마나 자연스럽게 이어지는지 계산한다.
     */
    private double calculateNextFlowScore(
            Node node,
            LocalDate date,
            List<LocalDate> scheduleDates,
            Map<LocalDate, DayBoundary> boundaryMap,
            Map<Long, Node> nodeMap
    ) {
        LocalDate nextDate = getNextDate(scheduleDates, date);

        if (nextDate == null) {
            return 0.0;
        }

        DayBoundary nextBoundary = boundaryMap.get(nextDate);

        if (nextBoundary == null || nextBoundary.effectiveStartNodeId() == null) {
            return 0.0;
        }

        Node nextStartNode = nodeMap.get(nextBoundary.effectiveStartNodeId());

        if (nextStartNode == null) {
            return 0.0;
        }

        return distanceKm(node, nextStartNode);
    }

    /**
     * 날짜별 불균형은 약하게만 반영한다.
     * 5:1 같은 분배도 동선상 자연스러우면 허용하기 위함이다.
     */
    private double calculateWeakBalancePenalty(
            LocalDate date,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            int maxNodesPerDay
    ) {
        int currentCount = plannedNodesByDate.getOrDefault(date, List.of()).size();

        int overCount = Math.max(0, currentCount - maxNodesPerDay + 1);

        return currentCount * 0.5
                + overCount * overCount * 5.0;
    }

    private LocalDate getPreviousDate(
            List<LocalDate> scheduleDates,
            LocalDate date
    ) {
        int index = scheduleDates.indexOf(date);

        if (index <= 0) {
            return null;
        }

        return scheduleDates.get(index - 1);
    }

    private LocalDate getNextDate(
            List<LocalDate> scheduleDates,
            LocalDate date
    ) {
        int index = scheduleDates.indexOf(date);

        if (index < 0 || index >= scheduleDates.size() - 1) {
            return null;
        }

        return scheduleDates.get(index + 1);
    }

    /**
     * 날짜별 boundary를 추론한다.
     *
     * 핵심:
     * - fixed start/end가 있으면 그것을 우선한다.
     * - fixed start가 없고 이전 날짜 end가 있으면, 이전 날짜 end와 가까운 노드를 start로 추론한다.
     * - fixed end가 없으면, start에서 가장 먼 노드를 end로 추론한다.
     */
    private Map<LocalDate, DayBoundary> inferBoundaries(
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            Map<LocalDate, AutoArrangeRequest.DayCondition> conditionMap
    ) {
        Map<LocalDate, DayBoundary> boundaryMap = new TreeMap<>();

        Node previousDayEndNode = null;

        for (LocalDate date : scheduleDates) {
            List<Node> dayNodes = plannedNodesByDate.getOrDefault(date, List.of());

            AutoArrangeRequest.DayCondition condition = conditionMap.get(date);

            Long fixedStartNodeId = condition != null ? condition.getStartNodeId() : null;
            Long fixedEndNodeId = condition != null ? condition.getEndNodeId() : null;

            Node inferredStartNode = inferStartNode(
                    dayNodes,
                    fixedStartNodeId,
                    fixedEndNodeId,
                    previousDayEndNode
            );

            Node inferredEndNode = inferEndNode(
                    dayNodes,
                    fixedEndNodeId,
                    inferredStartNode
            );

            Long inferredStartNodeId = inferredStartNode != null ? inferredStartNode.getId() : null;
            Long inferredEndNodeId = inferredEndNode != null ? inferredEndNode.getId() : null;

            DayBoundary boundary = new DayBoundary(
                    date,
                    fixedStartNodeId,
                    fixedEndNodeId,
                    inferredStartNodeId,
                    inferredEndNodeId
            );

            boundaryMap.put(date, boundary);

            previousDayEndNode = fixedEndNodeId != null
                    ? findNodeById(dayNodes, fixedEndNodeId)
                    : inferredEndNode;
        }

        return boundaryMap;
    }

    private Node inferStartNode(
            List<Node> dayNodes,
            Long fixedStartNodeId,
            Long fixedEndNodeId,
            Node previousDayEndNode
    ) {
        if (dayNodes == null || dayNodes.isEmpty()) {
            return null;
        }

        if (fixedStartNodeId != null) {
            return findNodeById(dayNodes, fixedStartNodeId);
        }

        if (previousDayEndNode != null) {
            return dayNodes.stream()
                    .filter(node -> !Objects.equals(node.getId(), fixedEndNodeId))
                    .min(Comparator
                            .comparingDouble((Node node) -> distanceKm(previousDayEndNode, node))
                            .thenComparing(Node::getId))
                    .orElse(dayNodes.get(0));
        }

        return findMedoidNode(dayNodes);
    }

    private Node inferEndNode(
            List<Node> dayNodes,
            Long fixedEndNodeId,
            Node startNode
    ) {
        if (dayNodes == null || dayNodes.isEmpty()) {
            return null;
        }

        if (fixedEndNodeId != null) {
            return findNodeById(dayNodes, fixedEndNodeId);
        }

        if (startNode == null) {
            return dayNodes.get(dayNodes.size() - 1);
        }

        return dayNodes.stream()
                .filter(node -> !Objects.equals(node.getId(), startNode.getId()))
                .max(Comparator
                        .comparingDouble((Node node) -> distanceKm(startNode, node))
                        .thenComparing(Node::getId))
                .orElse(startNode);
    }

    /**
     * boundary 기준으로 AI 요청 전 임시 visitOrder를 부여한다.
     */
    private void applyInitialOrdersByBoundary(
            List<LocalDate> scheduleDates,
            Map<LocalDate, List<Node>> plannedNodesByDate,
            Map<LocalDate, DayBoundary> boundaryMap
    ) {
        for (LocalDate date : scheduleDates) {
            List<Node> dayNodes = new ArrayList<>(
                    plannedNodesByDate.getOrDefault(date, List.of())
            );

            if (dayNodes.isEmpty()) {
                continue;
            }

            DayBoundary boundary = boundaryMap.get(date);

            Long startNodeId = boundary != null ? boundary.effectiveStartNodeId() : null;
            Long endNodeId = boundary != null ? boundary.effectiveEndNodeId() : null;

            List<Node> orderedNodes = orderNodesByNearestNeighbor(
                    dayNodes,
                    startNodeId,
                    endNodeId
            );

            for (int i = 0; i < orderedNodes.size(); i++) {
                orderedNodes.get(i).updateVisitInfo(i + 1, date);
            }
        }
    }

    /**
     * 날짜 내부 임시 순서를 nearest neighbor 방식으로 계산한다.
     * AI가 최종 순서를 다시 추천하지만, Edge 생성과 AI 입력 품질을 위해 임시 순서를 잡는다.
     */
    private List<Node> orderNodesByNearestNeighbor(
            List<Node> dayNodes,
            Long startNodeId,
            Long endNodeId
    ) {
        if (dayNodes == null || dayNodes.isEmpty()) {
            return List.of();
        }

        Map<Long, Node> nodeMap = dayNodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        Node startNode = startNodeId != null ? nodeMap.get(startNodeId) : null;
        Node endNode = endNodeId != null ? nodeMap.get(endNodeId) : null;

        if (startNode == null) {
            startNode = findMedoidNode(dayNodes);
        }

        List<Node> orderedNodes = new ArrayList<>();
        Set<Long> visitedNodeIds = new HashSet<>();

        orderedNodes.add(startNode);
        visitedNodeIds.add(startNode.getId());

        Node currentNode = startNode;

        while (visitedNodeIds.size() < dayNodes.size()) {
            Node nextNode = findNearestUnvisitedNode(
                    currentNode,
                    dayNodes,
                    visitedNodeIds,
                    endNode
            );

            if (nextNode == null) {
                break;
            }

            orderedNodes.add(nextNode);
            visitedNodeIds.add(nextNode.getId());
            currentNode = nextNode;
        }

        if (endNode != null
                && orderedNodes.stream().anyMatch(node -> Objects.equals(node.getId(), endNode.getId()))) {
            orderedNodes.removeIf(node -> Objects.equals(node.getId(), endNode.getId()));
            orderedNodes.add(endNode);
        }

        return orderedNodes;
    }

    private Node findNearestUnvisitedNode(
            Node currentNode,
            List<Node> dayNodes,
            Set<Long> visitedNodeIds,
            Node endNode
    ) {
        long remainingNonEndNodeCount = dayNodes.stream()
                .filter(node -> !visitedNodeIds.contains(node.getId()))
                .filter(node -> endNode == null || !Objects.equals(node.getId(), endNode.getId()))
                .count();

        return dayNodes.stream()
                .filter(node -> !visitedNodeIds.contains(node.getId()))
                .filter(node -> {
                    if (endNode == null) {
                        return true;
                    }

                    boolean isEndNode = Objects.equals(node.getId(), endNode.getId());

                    return !isEndNode || remainingNonEndNodeCount == 0;
                })
                .min(Comparator
                        .comparingDouble((Node node) -> distanceKm(currentNode, node))
                        .thenComparing(Node::getId))
                .orElse(null);
    }

    private Node findMedoidNode(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        return nodes.stream()
                .min(Comparator
                        .comparingDouble((Node node) -> totalDistanceToGroup(node, nodes))
                        .thenComparing(Node::getId))
                .orElse(nodes.get(0));
    }

    private double totalDistanceToGroup(
            Node targetNode,
            List<Node> nodes
    ) {
        return nodes.stream()
                .filter(node -> !Objects.equals(node.getId(), targetNode.getId()))
                .mapToDouble(node -> distanceKm(targetNode, node))
                .sum();
    }

    private Node findNodeById(
            List<Node> nodes,
            Long nodeId
    ) {
        if (nodes == null || nodeId == null) {
            return null;
        }

        return nodes.stream()
                .filter(node -> Objects.equals(node.getId(), nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * targetNode가 특정 날짜 그룹과 얼마나 가까운지 계산한다.
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
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

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
            Map<LocalDate, DayBoundary> boundaryMap
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
                buildDayRequests(schedule, nodes, edges, boundaryMap);

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
     *
     * startNodeId/endNodeId는 fixed boundary가 있으면 fixed 값을 사용하고,
     * 없으면 서버가 추론한 inferred boundary를 사용한다.
     */
    private List<RouteRecommendAiRequest.DayRequest> buildDayRequests(
            Schedule schedule,
            List<Node> nodes,
            List<Edge> edges,
            Map<LocalDate, DayBoundary> boundaryMap
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
            DayBoundary boundary = boundaryMap.get(current);

            Long preferredStartNodeId = boundary != null ? boundary.effectiveStartNodeId() : null;
            Long preferredEndNodeId = boundary != null ? boundary.effectiveEndNodeId() : null;

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

            boolean startFixed = boundary != null && boundary.fixedStartNodeId() != null;
            boolean endFixed = boundary != null && boundary.fixedEndNodeId() != null;

            dayRequests.add(new RouteRecommendAiRequest.DayRequest(
                    current,
                    preferredStartNodeId,
                    preferredEndNodeId,
                    startFixed,
                    endFixed,
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
     * AI는 서버가 확정한 node별 visitDate를 변경하면 안 된다.
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
     * 사용자가 명시한 fixed start/end 조건만 검증한다.
     * 서버가 추론한 inferred start/end는 강제 검증하지 않는다.
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

    private boolean hasCoordinate(Node node) {
        return node.getAttraction() != null
                && node.getAttraction().getLatitude() != null
                && node.getAttraction().getLongitude() != null;
    }

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