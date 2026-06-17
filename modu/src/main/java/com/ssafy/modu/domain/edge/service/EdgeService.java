package com.ssafy.modu.domain.edge.service;

import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.repository.EdgeRepository;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.routecache.entity.AttractionRouteCache;
import com.ssafy.modu.domain.routecache.repository.AttractionRouteCacheRepository;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.external.kakao.KakaoMobilityClient;
import com.ssafy.modu.external.kakao.dto.RouteSummary;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EdgeService {

    private final EdgeRepository edgeRepository;
    private final NodeRepository nodeRepository;
    private final KakaoMobilityClient kakaoMobilityClient;
    private final AttractionRouteCacheRepository attractionRouteCacheRepository;

    @Transactional(readOnly = true)
    public List<Edge> getEdgesByScheduleId(Long scheduleId) {
        return edgeRepository.findByScheduleId(scheduleId);
    }

    /**
     * 현재 일정의 최종 visitOrder 기준으로 실제 응답에 필요한 인접 Edge만 조회한다.
     *
     * DB 조회는 fromNodeId IN, toNodeId IN 후보군으로 가져온 뒤,
     * 메모리에서 실제 인접 Edge만 한 번 더 필터링한다.
     */
    @Transactional(readOnly = true)
    public List<Edge> getActiveEdgesForSchedule(Schedule schedule) {
        if (schedule == null || schedule.getId() == null || schedule.getNodes() == null) {
            return List.of();
        }

        Map<LocalDate, List<Node>> nodesByDate = schedule.getNodes().stream()
                .filter(node -> node.getVisitDate() != null)
                .collect(Collectors.groupingBy(Node::getVisitDate));

        Set<Long> fromNodeIds = new HashSet<>();
        Set<Long> toNodeIds = new HashSet<>();
        Set<EdgeKey> activeEdgeKeys = new HashSet<>();

        for (List<Node> nodes : nodesByDate.values()) {
            List<Node> sortedNodes = nodes.stream()
                    .sorted(Comparator.comparing(Node::getVisitOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Node::getId))
                    .toList();

            for (int i = 0; i < sortedNodes.size() - 1; i++) {
                Long fromNodeId = sortedNodes.get(i).getId();
                Long toNodeId = sortedNodes.get(i + 1).getId();

                fromNodeIds.add(fromNodeId);
                toNodeIds.add(toNodeId);
                activeEdgeKeys.add(new EdgeKey(fromNodeId, toNodeId));
            }
        }

        if (fromNodeIds.isEmpty() || toNodeIds.isEmpty()) {
            return List.of();
        }

        return edgeRepository.findByScheduleIdAndFromNodeIdInAndToNodeIdIn(
                        schedule.getId(),
                        fromNodeIds,
                        toNodeIds
                )
                .stream()
                .filter(edge -> activeEdgeKeys.contains(
                        new EdgeKey(edge.getFromNode().getId(), edge.getToNode().getId())
                ))
                .toList();
    }

    /**
     * 특정 노드가 날짜를 가지게 되었을 때,
     * 같은 schedule + 같은 visitDate 안의 다른 노드들과 양방향 Edge를 생성한다.
     *
     * 주로 기존 전체 Edge 캐시 방식이 필요한 곳에서 사용한다.
     */
    public EdgeBatchCreateResult createEdgesForPlacedNode(Node node) {
        if (node.getId() == null || node.getVisitDate() == null) {
            return EdgeBatchCreateResult.empty();
        }

        return createMissingEdgesForDates(
                node.getSchedule(),
                List.of(node.getVisitDate())
        );
    }

    /**
     * 자동 배치처럼 여러 노드가 한 번에 날짜를 가지게 된 경우,
     * 노드마다 Edge를 재생성하지 않고 visitDate 기준으로 묶어서 날짜 단위로 누락 Edge를 생성한다.
     *
     * 이 메서드는 같은 날짜의 전체 노드쌍 양방향 Edge를 생성한다.
     * auto-arrange처럼 후보 간 이동시간이 많이 필요한 기능에서 사용한다.
     */
    public EdgeBatchCreateResult createMissingEdgesForNewlyPlacedNodes(Collection<Node> newlyPlacedNodes) {
        if (newlyPlacedNodes == null || newlyPlacedNodes.isEmpty()) {
            return EdgeBatchCreateResult.empty();
        }

        Map<Long, List<Node>> nodesByScheduleId = newlyPlacedNodes.stream()
                .filter(node -> node.getId() != null)
                .filter(node -> node.getVisitDate() != null)
                .collect(Collectors.groupingBy(node -> node.getSchedule().getId()));

        EdgeBatchCreateResult result = EdgeBatchCreateResult.empty();

        for (List<Node> scheduleNodes : nodesByScheduleId.values()) {
            Schedule schedule = scheduleNodes.get(0).getSchedule();

            List<LocalDate> dates = scheduleNodes.stream()
                    .map(Node::getVisitDate)
                    .distinct()
                    .toList();

            result = result.plus(createMissingEdgesForDates(schedule, dates));
        }

        return result;
    }

    /**
     * 노드의 날짜가 변경되었을 때 사용한다.
     *
     * 기존 Edge는 이동 시간/거리 캐시로 재사용할 수 있으므로 삭제하지 않는다.
     * 새 날짜 기준으로 필요한 전체 양방향 Edge가 없으면 추가 생성한다.
     */
    public void rebuildEdgesForMovedNode(Node node) {
        if (node.getId() == null) {
            return;
        }

        if (node.getVisitDate() != null) {
            createEdgesForPlacedNode(node);
        }
    }

    /**
     * placement 전용 Edge 생성.
     *
     * 전체 양방향 Edge를 만들지 않고,
     * 각 날짜의 최종 visitOrder 기준 인접 Edge만 생성한다.
     *
     * 예:
     * A -> B -> C -> D
     *
     * 생성 대상:
     * A -> B
     * B -> C
     * C -> D
     */
    public EdgeBatchCreateResult createMissingAdjacentEdgesForDates(
            Schedule schedule,
            Collection<LocalDate> visitDates
    ) {
        if (schedule == null || schedule.getId() == null || visitDates == null || visitDates.isEmpty()) {
            return EdgeBatchCreateResult.empty();
        }

        EdgeBatchCreateResult result = EdgeBatchCreateResult.empty();

        for (LocalDate visitDate : visitDates) {
            result = result.plus(createMissingAdjacentEdgesForDate(schedule, visitDate));
        }

        return result;
    }

    /**
     * 관련된 노드가 삭제되거나 미배치 상태가 되기 전에,
     * 해당 노드와 연결된 Edge를 삭제한다.
     */
    public void deleteEdgesByNodeId(Long nodeId) {
        edgeRepository.deleteAllByNodeId(nodeId);
    }

    public void deleteEdgesByScheduleId(Long scheduleId) {
        edgeRepository.deleteAllByScheduleId(scheduleId);
    }

    /**
     * 날짜 단위로 전체 노드를 조회한 뒤,
     * 같은 날짜의 전체 노드쌍 양방향 Edge 중 누락된 Edge만 생성한다.
     *
     * placement보다는 auto-arrange처럼 후보 간 이동시간이 많이 필요한 기능에서 사용한다.
     */
    private EdgeBatchCreateResult createMissingEdgesForDates(
            Schedule schedule,
            Collection<LocalDate> visitDates
    ) {
        if (schedule == null || schedule.getId() == null || visitDates == null || visitDates.isEmpty()) {
            return EdgeBatchCreateResult.empty();
        }

        EdgeBatchCreateResult result = EdgeBatchCreateResult.empty();

        for (LocalDate visitDate : visitDates) {
            result = result.plus(createMissingEdgesForDate(schedule, visitDate));
        }

        return result;
    }

    private EdgeBatchCreateResult createMissingEdgesForDate(
            Schedule schedule,
            LocalDate visitDate
    ) {
        if (visitDate == null) {
            return EdgeBatchCreateResult.empty();
        }

        List<Node> sameDateNodes = nodeRepository
                .findWithAttractionBySchedule_IdAndVisitDate(schedule.getId(), visitDate);

        if (sameDateNodes.size() < 2) {
            return EdgeBatchCreateResult.empty();
        }

        List<Long> nodeIds = sameDateNodes.stream()
                .map(Node::getId)
                .toList();

        Set<EdgeKey> existingEdgeKeys = edgeRepository
                .findByScheduleIdAndFromNodeIdInAndToNodeIdIn(schedule.getId(), nodeIds, nodeIds)
                .stream()
                .map(edge -> new EdgeKey(edge.getFromNode().getId(), edge.getToNode().getId()))
                .collect(Collectors.toSet());

        int edgeCandidateCount = sameDateNodes.size() * (sameDateNodes.size() - 1);
        int existingEdgeCount = existingEdgeKeys.size();

        List<EdgeCandidate> candidates = buildMissingEdgeCandidates(
                schedule,
                sameDateNodes,
                existingEdgeKeys
        );

        return createEdgesFromCandidates(
                schedule,
                edgeCandidateCount,
                existingEdgeCount,
                candidates
        );
    }

    /**
     * 특정 날짜의 최종 visitOrder 기준 인접 Edge만 생성한다.
     */
    private EdgeBatchCreateResult createMissingAdjacentEdgesForDate(
            Schedule schedule,
            LocalDate visitDate
    ) {
        if (visitDate == null) {
            return EdgeBatchCreateResult.empty();
        }

        List<Node> sameDateNodes = nodeRepository
                .findWithAttractionBySchedule_IdAndVisitDate(schedule.getId(), visitDate)
                .stream()
                .sorted(Comparator.comparing(Node::getVisitOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Node::getId))
                .toList();

        if (sameDateNodes.size() < 2) {
            return EdgeBatchCreateResult.empty();
        }

        List<EdgeCandidate> adjacentCandidates = new ArrayList<>();
        Set<Long> fromNodeIds = new HashSet<>();
        Set<Long> toNodeIds = new HashSet<>();
        Set<EdgeKey> adjacentEdgeKeys = new HashSet<>();

        for (int i = 0; i < sameDateNodes.size() - 1; i++) {
            Node fromNode = sameDateNodes.get(i);
            Node toNode = sameDateNodes.get(i + 1);

            validateEdgeCreatable(schedule, fromNode, toNode);

            EdgeKey edgeKey = new EdgeKey(fromNode.getId(), toNode.getId());

            adjacentCandidates.add(new EdgeCandidate(fromNode, toNode));
            adjacentEdgeKeys.add(edgeKey);
            fromNodeIds.add(fromNode.getId());
            toNodeIds.add(toNode.getId());
        }

        Set<EdgeKey> existingEdgeKeys = edgeRepository
                .findByScheduleIdAndFromNodeIdInAndToNodeIdIn(
                        schedule.getId(),
                        fromNodeIds,
                        toNodeIds
                )
                .stream()
                .map(edge -> new EdgeKey(edge.getFromNode().getId(), edge.getToNode().getId()))
                .filter(adjacentEdgeKeys::contains)
                .collect(Collectors.toSet());

        List<EdgeCandidate> missingCandidates = adjacentCandidates.stream()
                .filter(candidate -> !existingEdgeKeys.contains(
                        new EdgeKey(candidate.fromNode().getId(), candidate.toNode().getId())
                ))
                .toList();

        return createEdgesFromCandidates(
                schedule,
                adjacentCandidates.size(),
                existingEdgeKeys.size(),
                missingCandidates
        );
    }

    private List<EdgeCandidate> buildMissingEdgeCandidates(
            Schedule schedule,
            List<Node> sameDateNodes,
            Set<EdgeKey> existingEdgeKeys
    ) {
        List<EdgeCandidate> candidates = new ArrayList<>();

        for (Node fromNode : sameDateNodes) {
            for (Node toNode : sameDateNodes) {
                if (fromNode.getId().equals(toNode.getId())) {
                    continue;
                }

                validateEdgeCreatable(schedule, fromNode, toNode);

                EdgeKey edgeKey = new EdgeKey(fromNode.getId(), toNode.getId());

                if (existingEdgeKeys.contains(edgeKey)) {
                    continue;
                }

                candidates.add(new EdgeCandidate(fromNode, toNode));
            }
        }

        return candidates;
    }

    /**
     * EdgeCandidate 목록을 실제 Edge로 저장한다.
     *
     * 관광지 경로 캐시가 있으면 캐시를 사용하고,
     * 없으면 카카오 길찾기 API를 호출한 뒤 AttractionRouteCache와 Edge를 저장한다.
     */
    private EdgeBatchCreateResult createEdgesFromCandidates(
            Schedule schedule,
            int edgeCandidateCount,
            int existingEdgeCount,
            List<EdgeCandidate> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return new EdgeBatchCreateResult(
                    edgeCandidateCount,
                    existingEdgeCount,
                    0,
                    0,
                    0,
                    0
            );
        }

        Map<RouteKey, AttractionRouteCache> routeCacheMap = loadRouteCacheMap(candidates);

        List<Edge> edgesToSave = new ArrayList<>();
        int routeCacheHitCount = 0;
        int routeCacheMissCount = 0;
        int kakaoApiCallCount = 0;

        for (EdgeCandidate candidate : candidates) {
            RouteKey routeKey = RouteKey.from(candidate.fromNode(), candidate.toNode());
            AttractionRouteCache cachedRoute = routeCacheMap.get(routeKey);

            RouteSummary routeSummary;

            if (cachedRoute != null) {
                routeCacheHitCount++;
                routeSummary = new RouteSummary(
                        cachedRoute.getDistanceMeters(),
                        cachedRoute.getDurationMinutes() * 60
                );
            } else {
                routeCacheMissCount++;
                routeSummary = getRouteSummaryFromKakao(candidate.fromNode(), candidate.toNode());
                kakaoApiCallCount++;

                AttractionRouteCache newCache = AttractionRouteCache.create(
                        candidate.fromNode().getAttraction(),
                        candidate.toNode().getAttraction(),
                        AttractionRouteCache.PROVIDER_KAKAO,
                        routeSummary.getDistanceMeters(),
                        routeSummary.getDurationMinutes()
                );

                AttractionRouteCache savedCache = attractionRouteCacheRepository.save(newCache);
                routeCacheMap.put(routeKey, savedCache);
            }

            edgesToSave.add(Edge.create(
                    schedule,
                    candidate.fromNode(),
                    candidate.toNode(),
                    routeSummary.getDurationMinutes(),
                    routeSummary.getDistanceMeters()
            ));
        }

        edgeRepository.saveAll(edgesToSave);

        return new EdgeBatchCreateResult(
                edgeCandidateCount,
                existingEdgeCount,
                edgesToSave.size(),
                routeCacheHitCount,
                routeCacheMissCount,
                kakaoApiCallCount
        );
    }

    private Map<RouteKey, AttractionRouteCache> loadRouteCacheMap(List<EdgeCandidate> candidates) {
        Set<Long> attractionIds = candidates.stream()
                .flatMap(candidate -> java.util.stream.Stream.of(
                        candidate.fromNode().getAttraction().getId(),
                        candidate.toNode().getAttraction().getId()
                ))
                .collect(Collectors.toSet());

        if (attractionIds.isEmpty()) {
            return new HashMap<>();
        }

        return attractionRouteCacheRepository
                .findByProviderAndFromAttraction_IdInAndToAttraction_IdIn(
                        AttractionRouteCache.PROVIDER_KAKAO,
                        attractionIds,
                        attractionIds
                )
                .stream()
                .collect(Collectors.toMap(
                        cache -> new RouteKey(
                                cache.getFromAttraction().getId(),
                                cache.getToAttraction().getId()
                        ),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    /**
     * 엣지 생성 정합성 판단.
     */
    private void validateEdgeCreatable(
            Schedule schedule,
            Node fromNode,
            Node toNode
    ) {
        if (fromNode.getId().equals(toNode.getId())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        if (fromNode.getVisitDate() == null || toNode.getVisitDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        if (!fromNode.getVisitDate().equals(toNode.getVisitDate())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        if (!fromNode.getSchedule().getId().equals(schedule.getId())
                || !toNode.getSchedule().getId().equals(schedule.getId())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }
    }

    /**
     * 카카오 모빌리티 API로 간선 정보를 조회한다.
     */
    private RouteSummary getRouteSummaryFromKakao(Node fromNode, Node toNode) {
        BigDecimal originLongitude = fromNode.getAttraction().getLongitude();
        BigDecimal originLatitude = fromNode.getAttraction().getLatitude();

        BigDecimal destinationLongitude = toNode.getAttraction().getLongitude();
        BigDecimal destinationLatitude = toNode.getAttraction().getLatitude();

        if (originLongitude == null || originLatitude == null
                || destinationLongitude == null || destinationLatitude == null) {
            throw new BusinessException(ErrorCode.INVALID_ATTRACTION_LOCATION);
        }

        return kakaoMobilityClient.getDirections(
                originLongitude,
                originLatitude,
                destinationLongitude,
                destinationLatitude
        );
    }

    public record EdgeBatchCreateResult(
            int edgeCandidateCount,
            int existingEdgeCount,
            int createdEdgeCount,
            int routeCacheHitCount,
            int routeCacheMissCount,
            int kakaoApiCallCount
    ) {
        public static EdgeBatchCreateResult empty() {
            return new EdgeBatchCreateResult(0, 0, 0, 0, 0, 0);
        }

        public EdgeBatchCreateResult plus(EdgeBatchCreateResult other) {
            if (other == null) {
                return this;
            }

            return new EdgeBatchCreateResult(
                    this.edgeCandidateCount + other.edgeCandidateCount,
                    this.existingEdgeCount + other.existingEdgeCount,
                    this.createdEdgeCount + other.createdEdgeCount,
                    this.routeCacheHitCount + other.routeCacheHitCount,
                    this.routeCacheMissCount + other.routeCacheMissCount,
                    this.kakaoApiCallCount + other.kakaoApiCallCount
            );
        }
    }

    private record EdgeCandidate(Node fromNode, Node toNode) {
    }

    private record EdgeKey(Long fromNodeId, Long toNodeId) {
    }

    private record RouteKey(Long fromAttractionId, Long toAttractionId) {
        private static RouteKey from(Node fromNode, Node toNode) {
            return new RouteKey(
                    fromNode.getAttraction().getId(),
                    toNode.getAttraction().getId()
            );
        }
    }
}