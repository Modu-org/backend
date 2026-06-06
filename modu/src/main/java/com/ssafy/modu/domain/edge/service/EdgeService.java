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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
     * 특정 노드가 날짜를 가지게 되었을 때,
     * 같은 schedule + 같은 visitDate 안의 다른 노드들과 양방향 Edge를 생성한다.
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
     * 기존 날짜에서 연결된 Edge를 삭제하고,
     * 새 날짜 기준으로 다시 Edge를 만든다.
     */
    public void rebuildEdgesForMovedNode(Node node) {
        if (node.getId() == null) {
            return;
        }
        // 노드의 날짜가 변경되면, 원래 날짜의 노드는 삭제함
        edgeRepository.deleteAllByNodeId(node.getId());

        // 바뀐 날짜가 null이 아니면 이 노드와 같은 그룹인 노드들끼리 새로운 간선을 만듦
        if (node.getVisitDate() != null) {
            createEdgesForPlacedNode(node);
        }
    }

    /**
     * 관련된 노드가 삭제되기 전에, 엣지부터 삭제되어야 함
     */
    public void deleteEdgesByNodeId(Long nodeId) {
        edgeRepository.deleteAllByNodeId(nodeId);
    }

    /**
     * 날짜 단위로 전체 노드를 조회한 뒤, 기존 Edge와 관광지 경로 캐시를 일괄 조회해서 누락된 Edge만 생성한다.
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

        List<EdgeCandidate> candidates = buildMissingEdgeCandidates(schedule, sameDateNodes, existingEdgeKeys);

        if (candidates.isEmpty()) {
            EdgeBatchCreateResult result = new EdgeBatchCreateResult(
                    edgeCandidateCount,
                    existingEdgeCount,
                    0,
                    0,
                    0,
                    0
            );

            log.info(
                    "edge batch create: scheduleId={}, visitDate={}, nodeCount={}, edgeCandidateCount={}, existingEdgeCount={}, createdEdgeCount={}, routeCacheHitCount={}, routeCacheMissCount={}, kakaoApiCallCount={}",
                    schedule.getId(),
                    visitDate,
                    sameDateNodes.size(),
                    result.edgeCandidateCount(),
                    result.existingEdgeCount(),
                    result.createdEdgeCount(),
                    result.routeCacheHitCount(),
                    result.routeCacheMissCount(),
                    result.kakaoApiCallCount()
            );

            return result;
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

        EdgeBatchCreateResult result = new EdgeBatchCreateResult(
                edgeCandidateCount,
                existingEdgeCount,
                edgesToSave.size(),
                routeCacheHitCount,
                routeCacheMissCount,
                kakaoApiCallCount
        );

        log.info(
                "edge batch create: scheduleId={}, visitDate={}, nodeCount={}, edgeCandidateCount={}, existingEdgeCount={}, createdEdgeCount={}, routeCacheHitCount={}, routeCacheMissCount={}, kakaoApiCallCount={}",
                schedule.getId(),
                visitDate,
                sameDateNodes.size(),
                result.edgeCandidateCount(),
                result.existingEdgeCount(),
                result.createdEdgeCount(),
                result.routeCacheHitCount(),
                result.routeCacheMissCount(),
                result.kakaoApiCallCount()
        );

        return result;
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
     * 만약에 이미 만들어진 엣지면 그냥 넘어가고, 그렇지 않은 경우에 대해서만 엣지 생성
     */
    private void createEdgeIfAbsent(
            Schedule schedule,
            Node fromNode,
            Node toNode
    ) {
        validateEdgeCreatable(schedule, fromNode, toNode);

        // 지금 생성하려는 엣지 정보(시작노드, 끝노드, 스케줄 아이디)를 보고, 이미 존재하는 엣지인지 판단
        boolean exists = edgeRepository.existsByScheduleIdAndFromNodeIdAndToNodeId(
                schedule.getId(),
                fromNode.getId(),
                toNode.getId()
        );

        if (exists) {
            return;
        }
        // 존재하지 않는 엣지에 한해서, 카카오 모빌리티 api를 호출해서 간선을 만듦
        RouteSummary routeSummary = getRouteSummary(fromNode, toNode);

        Edge edge = Edge.create(
                schedule,
                fromNode,
                toNode,
                routeSummary.getDurationMinutes(),
                routeSummary.getDistanceMeters()
        );

        edgeRepository.save(edge);
    }

    /**
     * 엣지 생성 정합성 판단
     */
    private void validateEdgeCreatable(
            Schedule schedule,
            Node fromNode,
            Node toNode
    ) {
        // 시작이랑 끝노드 같으면 안됨
        if (fromNode.getId().equals(toNode.getId())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        // 노드 둘 중에 하나의 방문 날짜가 배정이 안되어있으면 실패
        if (fromNode.getVisitDate() == null || toNode.getVisitDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        // 시작노드랑 끝노드의 방문 날짜가 다르면 실패
        if (!fromNode.getVisitDate().equals(toNode.getVisitDate())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }

        // 지금 간선을 만들고 있는 스케줄 아이디랑, 실제 작업하고 있는 스케줄 아이디가 서로 다르면 안됨
        if (!fromNode.getSchedule().getId().equals(schedule.getId())
                || !toNode.getSchedule().getId().equals(schedule.getId())) {
            throw new BusinessException(ErrorCode.INVALID_EDGE_REQUEST);
        }
    }

    // 관광지 이동 캐시를 우선 사용하고, 캐시가 없을 때만 카카오 모빌리티 api로 간선 정보 조회
    private RouteSummary getRouteSummary(Node fromNode, Node toNode) {
        RouteKey routeKey = RouteKey.from(fromNode, toNode);

        return attractionRouteCacheRepository
                .findByFromAttraction_IdAndToAttraction_IdAndProvider(
                        routeKey.fromAttractionId(),
                        routeKey.toAttractionId(),
                        AttractionRouteCache.PROVIDER_KAKAO
                )
                .map(cache -> new RouteSummary(
                        cache.getDistanceMeters(),
                        cache.getDurationMinutes() * 60
                ))
                .orElseGet(() -> {
                    RouteSummary routeSummary = getRouteSummaryFromKakao(fromNode, toNode);

                    AttractionRouteCache cache = AttractionRouteCache.create(
                            fromNode.getAttraction(),
                            toNode.getAttraction(),
                            AttractionRouteCache.PROVIDER_KAKAO,
                            routeSummary.getDistanceMeters(),
                            routeSummary.getDurationMinutes()
                    );

                    attractionRouteCacheRepository.save(cache);
                    return routeSummary;
                });
    }

    // 카카오 모빌리티 api로 간선 정보 조회
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

