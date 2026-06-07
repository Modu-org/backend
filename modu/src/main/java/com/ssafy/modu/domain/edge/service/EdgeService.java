package com.ssafy.modu.domain.edge.service;

import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.repository.EdgeRepository;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EdgeService {

    private final EdgeRepository edgeRepository;
    private final NodeRepository nodeRepository;
    private final KakaoMobilityClient kakaoMobilityClient;


    @Transactional(readOnly = true)
    public List<Edge> getEdgesByScheduleId(Long scheduleId) {
        return edgeRepository.findByScheduleId(scheduleId);
    }

    /**
     * 특정 노드가 날짜를 가지게 되었을 때,
     * 같은 schedule + 같은 visitDate 안의 다른 노드들과 양방향 Edge를 생성한다.
     */
    public void createEdgesForPlacedNode(Node node) {
        if (node.getId() == null || node.getVisitDate() == null) {
            return;
        }

        Schedule schedule = node.getSchedule();
        LocalDate visitDate = node.getVisitDate();

        // 그 노드와 같은 그룹(스케줄 아이디 같고, 날짜 같은)인 노드들을 가져옴
        List<Node> sameDateNodes = nodeRepository
                .findWithAttractionBySchedule_IdAndVisitDate(schedule.getId(), visitDate);

        // 이번에 새로 추가된 노드와 다른 노드들 각각의 엣지를 생성 (이미 있는 엣지면 생성 안함)
        for (Node otherNode : sameDateNodes) {
            if (otherNode.getId().equals(node.getId())) {
                continue;
            }

            createEdgeIfAbsent(schedule, node, otherNode);
            createEdgeIfAbsent(schedule, otherNode, node);
        }
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
    public void deleteEdgesByScheduleId(Long scheduleId) {
        edgeRepository.deleteAllByScheduleId(scheduleId);
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

    // 카카오 모빌리티 api로 간선 정보 저장
    private RouteSummary getRouteSummary(Node fromNode, Node toNode) {
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
}