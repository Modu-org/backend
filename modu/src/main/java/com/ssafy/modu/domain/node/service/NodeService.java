package com.ssafy.modu.domain.node.service;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.ranking.event.AttractionAddedToScheduleEvent;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.service.EdgeService;
import com.ssafy.modu.domain.node.dto.request.NodeArrangementRequest;
import com.ssafy.modu.domain.node.dto.request.NodeCreateRequest;
import com.ssafy.modu.domain.node.dto.response.NodeDetailResponse;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NodeService {

    private final ScheduleRepository scheduleRepository;
    private final NodeRepository nodeRepository;
    private final AttractionRepository attractionRepository;
    private final EdgeService edgeService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 노드를 새로 생성해서 스케줄에 넣음.
     */
    @Transactional
    public NodeResponse addNode(Long userId, Long scheduleId, NodeCreateRequest request) {
        Schedule schedule = getSchedule(userId, scheduleId);

        if (request.getAttractionId() == null) {
            throw new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND);
        }

        Attraction attraction = attractionRepository.findById(request.getAttractionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND));

        Node node = Node.from(attraction);
        schedule.addNode(node);

        Node savedNode = nodeRepository.save(node);
        eventPublisher.publishEvent(
                new AttractionAddedToScheduleEvent(
                        attraction.getId(),
                        attraction.getLDongRegnCd()
                )
        );
        return NodeResponse.from(savedNode);
    }

    public NodeDetailResponse getNodeDetail(Long userId, Long scheduleId, Long nodeId) {
        getSchedule(userId, scheduleId);

        Node node = nodeRepository.findWithAttractionAccessibilityByIdAndSchedule_Id(nodeId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        return NodeDetailResponse.from(node);
    }

    /**
     * 특정 노드 삭제.
     */
    @Transactional
    public void deleteNode(Long userId, Long scheduleId, Long nodeId) {
        getSchedule(userId, scheduleId);

        Node node = nodeRepository.findByIdAndSchedule_Id(nodeId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        LocalDate deletedVisitDate = node.getVisitDate();

        // 노드를 삭제할 때, 이 노드를 시작이나 끝 노드로 가지고 있던 간선을 삭제
        edgeService.deleteEdgesByNodeId(nodeId);

        nodeRepository.delete(node);
        nodeRepository.flush();

        if (deletedVisitDate != null) {
            reorderNodesAfterDelete(scheduleId, deletedVisitDate);
        }
    }

    /**
     * 노드 재정렬.
     *
     * 같은 날짜 안에서 순서만 바뀐 노드:
     * - visitOrder만 변경
     * - Edge 변경 없음
     *
     * null 날짜였다가 새 날짜에 배치된 노드:
     * - 같은 날짜 노드들과 Edge 생성
     *
     * 기존 날짜에서 다른 날짜로 이동한 노드:
     * - 기존 Edge 삭제
     * - 새 날짜 기준 Edge 재생성
     */
    @Transactional
    public ScheduleDetailResponse updateNodeArrangement(
            Long userId,
            Long scheduleId,
            NodeArrangementRequest request
    ) {
        Schedule schedule = getScheduleWithNodes(userId, scheduleId);

        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusinessException(ErrorCode.NODE_ARRANGEMENT_EMPTY);
        }

        Map<Long, Node> nodeMap = schedule.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        Set<Long> requestedNodeIds = new HashSet<>();

        /*
         * placement에서는 전체 양방향 Edge를 만들지 않는다.
         * 변경된 날짜의 최종 visitOrder 기준 인접 Edge만 보장한다.
         */
        Set<LocalDate> affectedDates = new HashSet<>();
        List<Node> unplacedNodes = new ArrayList<>();

        for (NodeArrangementRequest.DayArrangement day : request.getDays()) {
            validateVisitDate(schedule, day.getDate());

            if (day.getNodes() == null) {
                continue;
            }

            validateArrangementRule(day);

            if (day.getDate() != null) {
                validateDuplicateVisitOrder(day.getNodes());
            }

            for (NodeArrangementRequest.NodeArrangement item : day.getNodes()) {
                if (item.getNodeId() == null) {
                    throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
                }

                if (!requestedNodeIds.add(item.getNodeId())) {
                    throw new BusinessException(ErrorCode.DUPLICATE_NODE_IN_REQUEST);
                }

                Node node = nodeMap.get(item.getNodeId());

                if (node == null) {
                    throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
                }

                LocalDate beforeVisitDate = node.getVisitDate();
                LocalDate afterVisitDate = day.getDate();

                boolean dateChanged = !Objects.equals(beforeVisitDate, afterVisitDate);
                boolean orderChanged = !Objects.equals(node.getVisitOrder(), item.getVisitOrder());

                if (!dateChanged && !orderChanged) {
                    continue;
                }

                node.updateVisitInfo(
                        item.getVisitOrder(),
                        afterVisitDate
                );

                /*
                 * 날짜가 바뀌면 기존 날짜와 새 날짜 모두 인접 Edge 재확인이 필요하다.
                 * 예: A -> B -> C 에서 B가 빠지면 A -> C가 새로 필요할 수 있다.
                 */
                if (dateChanged) {
                    if (beforeVisitDate != null) {
                        affectedDates.add(beforeVisitDate);
                    }

                    if (afterVisitDate != null) {
                        affectedDates.add(afterVisitDate);
                    } else {
                        unplacedNodes.add(node);
                    }
                }

                /*
                 * 같은 날짜 안에서 순서만 바뀌어도 새 인접 Edge가 필요할 수 있다.
                 * 예: A -> B -> C 를 A -> C -> B 로 바꾸면 A -> C, C -> B가 필요하다.
                 */
                if (orderChanged && afterVisitDate != null) {
                    affectedDates.add(afterVisitDate);
                }
            }
        }

        /*
         * affectedDates가 있으면 최신 visitDate / visitOrder 기준으로
         * 인접 Edge를 계산해야 하므로 flush가 필요하다.
         */
        if (!affectedDates.isEmpty() || !unplacedNodes.isEmpty()) {
            nodeRepository.flush();

            for (Node node : unplacedNodes) {
                edgeService.deleteEdgesByNodeId(node.getId());
            }

            edgeService.createMissingAdjacentEdgesForDates(schedule, affectedDates);
        }

        List<Edge> activeEdges = edgeService.getActiveEdgesForSchedule(schedule);
        return ScheduleDetailResponse.from(schedule, activeEdges);
    }

    private Schedule getScheduleWithNodes(Long userId, Long scheduleId) {
        return scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Schedule getSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateVisitDate(Schedule schedule, LocalDate visitDate) {
        if (visitDate == null) {
            return; // 미배정 그룹 허용
        }

        if (visitDate.isBefore(schedule.getStartDate()) || visitDate.isAfter(schedule.getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_NODE_VISIT_DATE);
        }
    }

    private void reorderNodesAfterDelete(Long scheduleId, LocalDate visitDate) {
        List<Node> nodes = nodeRepository
                .findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(scheduleId, visitDate);

        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).updateVisitInfo(i + 1, visitDate);
        }
    }

    private void validateDuplicateVisitOrder(List<NodeArrangementRequest.NodeArrangement> nodes) {
        Set<Integer> visitOrders = new HashSet<>();

        for (NodeArrangementRequest.NodeArrangement node : nodes) {
            if (node.getVisitOrder() == null || node.getVisitOrder() < 1) {
                throw new BusinessException(ErrorCode.INVALID_NODE_VISIT_ORDER);
            }

            if (!visitOrders.add(node.getVisitOrder())) {
                throw new BusinessException(ErrorCode.DUPLICATE_VISIT_ORDER);
            }
        }
    }

    private void validateArrangementRule(NodeArrangementRequest.DayArrangement day) {
        for (NodeArrangementRequest.NodeArrangement nodeRequest : day.getNodes()) {
            if (day.getDate() != null && nodeRequest.getVisitOrder() == null) {
                throw new BusinessException(ErrorCode.VISIT_ORDER_REQUIRED);
            }

            if (day.getDate() == null && nodeRequest.getVisitOrder() != null) {
                throw new BusinessException(ErrorCode.INVALID_UNASSIGNED_NODE_ORDER);
            }
        }
    }

}