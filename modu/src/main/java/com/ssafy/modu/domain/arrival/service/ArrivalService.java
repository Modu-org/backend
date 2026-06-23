package com.ssafy.modu.domain.arrival.service;

import com.ssafy.modu.domain.arrival.dto.request.ArrivalRequest;
import com.ssafy.modu.domain.arrival.dto.response.ArrivalLogDetailResponse;
import com.ssafy.modu.domain.arrival.dto.response.ArrivalResponse;
import com.ssafy.modu.domain.arrival.dto.response.NextDestinationResponse;
import com.ssafy.modu.domain.arrival.entity.ArrivalLog;
import com.ssafy.modu.domain.arrival.repository.ArrivalLogRepository;
import com.ssafy.modu.domain.arrival.util.DistanceCalculator;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.caregiver.repository.CaregiverRelationRepository;
import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.repository.EdgeRepository;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArrivalService {

    private static final double ARRIVAL_THRESHOLD_METERS = 100.0;

    private final ScheduleRepository scheduleRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final ArrivalLogRepository arrivalLogRepository;
    private final DistanceCalculator distanceCalculator;
    private final ArrivalNotificationService arrivalNotificationService;
    private final CaregiverRelationRepository caregiverRelationRepository;

    // 도착 처리 로직
    public ArrivalResponse confirmArrival(
            Long userId,
            Long scheduleId,
            Long nodeId,
            ArrivalRequest request
    ) {
        // 스케줄과 노드의 정합성 검사
        Schedule schedule = scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Node currentNode = nodeRepository.findByIdAndSchedule_Id(nodeId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        validateArrivableNode(currentNode);

        Attraction currentAttraction = currentNode.getAttraction();

        Optional<ArrivalLog> successLogOptional =
                arrivalLogRepository.findFirstByScheduleIdAndNodeIdAndArrivedTrueOrderByRequestedAtDesc(
                        scheduleId,
                        nodeId
                );

        // 이미 도착 성공한 노드에 대해서 여러번 누를 시의 예외 처리
        if (successLogOptional.isPresent()) {
            ArrivalLog successLog = successLogOptional.get();

            Node nextNode = findNextNode(currentNode);

            NextDestinationResponse nextDestination = null;
            if (nextNode != null) {
                nextDestination = createNextDestination(scheduleId, currentNode, nextNode);
            }

            return ArrivalResponse.alreadyArrived(
                    currentAttraction.getName(),
                    successLog.getDistanceMeters(),
                    nextDestination
            );
        }

        double attractionLatitude = toNullableDouble(currentAttraction.getLatitude());
        double attractionLongitude = toNullableDouble(currentAttraction.getLongitude());

        // 도착 버튼을 누른 시점의 위치정보를 이용해서, 관광지와의 거리 계산
        double distanceMeters = distanceCalculator.calculateMeters(
                request.getLatitude(),
                request.getLongitude(),
                attractionLatitude,
                attractionLongitude
        );

        // 도착 여부 판단
        boolean arrived = distanceMeters <= ARRIVAL_THRESHOLD_METERS;

        // 도착 여부 판단 내역과 함께, 이번 이벤트 저장
        ArrivalLog arrivalLog = ArrivalLog.create(
                scheduleId,
                nodeId,
                userId,
                arrived,
                request.getLatitude(),
                request.getLongitude(),
                attractionLatitude,
                attractionLongitude,
                distanceMeters
        );

        ArrivalLog savedArrivalLog = arrivalLogRepository.save(arrivalLog);

        // 다음 목적지 찾음
        Node nextNode = findNextNode(currentNode);

        NextDestinationResponse nextDestination = null;

        // 도착도 정상적으로 했고, 다음 목적지도 있다면 다음 목적지에 대한 정보 저장
        if (arrived && nextNode != null) {
            nextDestination = createNextDestination(scheduleId, currentNode, nextNode);
        }

        // 도착 이벤트를 보호자에게 알림으로 넘김
        arrivalNotificationService.notifyCaregivers(
                schedule,
                currentNode,
                nextNode,
                savedArrivalLog,
                arrived,
                distanceMeters
        );

        return ArrivalResponse.of(
                arrived,
                currentAttraction.getName(),
                distanceMeters,
                nextDestination
        );
    }
    @Transactional(readOnly = true)
    public ArrivalLogDetailResponse getArrivalLogDetail(
            Long userId,
            Long arrivalLogId
    ) {
        ArrivalLog arrivalLog = arrivalLogRepository.findById(arrivalLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARRIVAL_LOG_NOT_FOUND));

        Schedule schedule = scheduleRepository.findById(arrivalLog.getScheduleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        validateArrivalLogAccess(userId, arrivalLog, schedule);

        Node node = nodeRepository.findByIdAndSchedule_Id(
                        arrivalLog.getNodeId(),
                        arrivalLog.getScheduleId()
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));

        Attraction attraction = node.getAttraction();

        Node nextNode = findNextNode(node);

        NextDestinationResponse nextDestination = null;
        if (nextNode != null && nextNode.getAttraction() != null) {
            nextDestination = createNextDestination(
                    arrivalLog.getScheduleId(),
                    node,
                    nextNode
            );
        }

        return ArrivalLogDetailResponse.of(
                arrivalLog,
                node,
                attraction,
                nextDestination
        );
    }
    private void validateArrivalLogAccess(
            Long userId,
            ArrivalLog arrivalLog,
            Schedule schedule
    ) {
        Long travelerId = arrivalLog.getTravelerId();

        if (travelerId.equals(userId)) {
            return;
        }

        boolean isAcceptedCaregiver = caregiverRelationRepository
                .existsByTravelerIdAndCaregiverIdAndActiveTrue(
                        travelerId,
                        userId
                );

        if (schedule.isArrivalShared() && isAcceptedCaregiver) {
            return;
        }

        throw new BusinessException(ErrorCode.ARRIVAL_LOG_NOT_FOUND);
    }
    // 도착 버튼을 누른 노드가 정상 노드인지 확인
    private void validateArrivableNode(Node node) {
        if (node.getVisitDate() == null || node.getVisitOrder() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARRIVAL_NODE);
        }

        if (node.getAttraction() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARRIVAL_NODE);
        }

        if (node.getAttraction().getLatitude() == null || node.getAttraction().getLongitude() == null) {
            throw new BusinessException(ErrorCode.INVALID_ATTRACTION_LOCATION);
        }
    }
    // 현재 도착지의 그다음 노드를 찾음
    private Node findNextNode(Node currentNode) {
        List<Node> nodes = nodeRepository.findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(
                currentNode.getSchedule().getId(),
                currentNode.getVisitDate()
        );

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(currentNode.getId())) {
                if (i + 1 >= nodes.size()) {
                    return null;
                }

                return nodes.get(i + 1);
            }
        }

        return null;
    }
    // 현재 노드의 그다음 도착지와 연결된 간선을 찾아서 다음 목적지 정보를 채워넣음
    private NextDestinationResponse createNextDestination(
            Long scheduleId,
            Node currentNode,
            Node nextNode
    ) {
        Edge edge = edgeRepository.findByScheduleIdAndFromNodeIdAndToNodeId(
                scheduleId,
                currentNode.getId(),
                nextNode.getId()
        ).orElse(null);

        Integer distanceMeters = edge != null ? edge.getDistanceMeters() : null;
        Integer estimatedTimeMinutes = edge != null ? edge.getEstimatedTimeMinutes() : null;

        Attraction nextAttraction = nextNode.getAttraction();

        return NextDestinationResponse.of(
                nextNode.getId(),
                nextAttraction.getId(),
                nextAttraction.getName(),
                nextAttraction.getAddress(),
                toNullableDouble(nextAttraction.getLatitude()),
                toNullableDouble(nextAttraction.getLongitude()),
                distanceMeters,
                estimatedTimeMinutes
        );
    }

    private Double toNullableDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}