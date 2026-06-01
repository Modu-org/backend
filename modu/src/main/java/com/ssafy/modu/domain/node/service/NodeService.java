package com.ssafy.modu.domain.node.service;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
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

    // 노드를 새로 생성해서 스케줄에 넣음 (방문일자와 순서는 null임)
    @Transactional
    public NodeResponse addNode(Long userId, Long scheduleId, NodeCreateRequest request) {
        Schedule schedule = getSchedule(userId, scheduleId);

        if (request.getAttractionId() == null) {
            throw new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND);
        }

        Attraction attraction = attractionRepository.findById(request.getAttractionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND));

        // Attraction을 이용해서 노드 생성
        Node node = Node.from(attraction);
        schedule.addNode(node);

        // 노드를 이용해서 응답 생성해서 반환
        return NodeResponse.from(nodeRepository.save(node));
    }
    // 노드의 상세 정보 조회
    public NodeDetailResponse getNodeDetail(Long userId, Long scheduleId, Long nodeId) {
        getSchedule(userId, scheduleId);

        Node node = nodeRepository.findWithAttractionAccessibilityByIdAndSchedule_Id(nodeId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        return NodeDetailResponse.from(node);
    }

    // 특정 노드 삭제
    @Transactional
    public void deleteNode(Long userId, Long scheduleId, Long nodeId) {
        getSchedule(userId, scheduleId);

        Node node = nodeRepository.findByIdAndSchedule_Id(nodeId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        // 삭제된 노드의 방문 일자를 가져옴
        LocalDate deletedVisitDate = node.getVisitDate();

        nodeRepository.delete(node);
        nodeRepository.flush();

        if (deletedVisitDate != null) {
            reorderNodesAfterDelete(scheduleId, deletedVisitDate);
        }
    }
    // 노드 재정렬 -> 프론트에서 들어온 순서 그대로 다시 저장
    @Transactional
    public ScheduleDetailResponse updateNodeArrangement(
            Long userId,
            Long scheduleId,
            NodeArrangementRequest request
    ) {
        Schedule schedule = getScheduleWithNodes(userId, scheduleId);

        // 배치 정보가 누락되면 에러 발생
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusinessException(ErrorCode.NODE_ARRANGEMENT_EMPTY);
        }

        // 노드 아이디로 노드를 바로 찾기 위한 map
        Map<Long, Node> nodeMap = schedule.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        // 중복된 노드ID가 있는지 확인 용도
        Set<Long> requestedNodeIds = new HashSet<>();

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

                node.updateVisitInfo(
                        item.getVisitOrder(),
                        day.getDate()
                );
            }
        }

        return ScheduleDetailResponse.from(schedule);
    }
    // 스케줄 + 노드 정보 가져옴
    private Schedule getScheduleWithNodes(Long userId, Long scheduleId) {
        return scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }
    // 스케줄 정보만 가져옴
    private Schedule getSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    // 노드 방문 일자가 노드가 포함된 스케줄의 여행 일자 안에 있는지 확인
    private void validateVisitDate(Schedule schedule, LocalDate visitDate) {
        if (visitDate == null) {
            return; // 미배정 그룹 허용
        }

        if (visitDate.isBefore(schedule.getStartDate()) || visitDate.isAfter(schedule.getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_NODE_VISIT_DATE);
        }
    }
    // 노드 삭제한 다음에, 노드 순서를 재배치하는 로직 (처음부터 다시 계산함)
    private void reorderNodesAfterDelete(Long scheduleId, LocalDate visitDate) {
        List<Node> nodes = nodeRepository
                .findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(scheduleId, visitDate);

        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).updateVisitInfo(i + 1, visitDate);
        }
    }
    // 중복된 방문순서가 있는지 검사
    private void validateDuplicateVisitOrder(List<NodeArrangementRequest.NodeArrangement> nodes) {
        Set<Integer> visitOrders = new HashSet<>();

        for (NodeArrangementRequest.NodeArrangement node : nodes) {
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