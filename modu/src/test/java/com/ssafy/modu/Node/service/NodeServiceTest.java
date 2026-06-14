package com.ssafy.modu.Node.service;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.node.dto.request.NodeArrangementRequest;
import com.ssafy.modu.domain.node.dto.request.NodeCreateRequest;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.node.service.NodeService;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @InjectMocks
    private NodeService nodeService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private AttractionRepository attractionRepository;

    @Test
    @DisplayName("관광지를 스케줄에 노드로 추가하면 visitDate와 visitOrder는 null 상태로 저장된다")
    void addNode_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;
        Long attractionId = 100L;

        Schedule schedule = createSchedule(scheduleId, userId);
        Attraction attraction = createAttraction(attractionId, "대구수목원");

        NodeCreateRequest request = new NodeCreateRequest();
        ReflectionTestUtils.setField(request, "attractionId", attractionId);

        when(scheduleRepository.findByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));
        when(attractionRepository.findById(attractionId))
                .thenReturn(Optional.of(attraction));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(invocation -> {
                    Node savedNode = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedNode, "id", 1000L);
                    return savedNode;
                });

        // when
        NodeResponse response = nodeService.addNode(userId, scheduleId, request);

        // then
        assertThat(response.getNodeId()).isEqualTo(1000L);
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getAttractionId()).isEqualTo(attractionId);
        assertThat(response.getVisitDate()).isNull();
        assertThat(response.getVisitOrder()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("대구수목원");

        verify(nodeRepository).save(any(Node.class));
    }

    @Test
    @DisplayName("프론트에서 받은 날짜별 노드 배치 정보를 그대로 visitDate와 visitOrder에 저장한다")
    void updateNodeArrangement_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, userId);

        Node node1 = createNode(1L, createAttraction(101L, "대구수목원"));
        Node node2 = createNode(2L, createAttraction(102L, "김광석거리"));
        Node node3 = createNode(3L, createAttraction(103L, "서문시장"));

        schedule.addNode(node1);
        schedule.addNode(node2);
        schedule.addNode(node3);

        NodeArrangementRequest request = createArrangementRequest(
                List.of(
                        createDay(
                                LocalDate.of(2026, 5, 10),
                                List.of(
                                        createNodeArrangement(1L, 1),
                                        createNodeArrangement(2L, 2)
                                )
                        ),
                        createDay(
                                LocalDate.of(2026, 5, 11),
                                List.of(
                                        createNodeArrangement(3L, 1)
                                )
                        )
                )
        );

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when
        ScheduleDetailResponse response = nodeService.updateNodeArrangement(userId, scheduleId, request);

        // then
        assertThat(node1.getVisitDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(node1.getVisitOrder()).isEqualTo(1);

        assertThat(node2.getVisitDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(node2.getVisitOrder()).isEqualTo(2);

        assertThat(node3.getVisitDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(node3.getVisitOrder()).isEqualTo(1);

        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getDays()).hasSize(2);
    }

    @Test
    @DisplayName("여행 기간 밖의 날짜로 노드를 배치하면 예외가 발생한다")
    void updateNodeArrangement_invalidVisitDate_fail() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, userId);
        Node node = createNode(1L, createAttraction(101L, "대구수목원"));
        schedule.addNode(node);

        NodeArrangementRequest request = createArrangementRequest(
                List.of(
                        createDay(
                                LocalDate.of(2026, 5, 12),
                                List.of(createNodeArrangement(1L, 1))
                        )
                )
        );

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> nodeService.updateNodeArrangement(userId, scheduleId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_NODE_VISIT_DATE);
    }

    @Test
    @DisplayName("요청에 같은 nodeId가 중복되면 예외가 발생한다")
    void updateNodeArrangement_duplicateNode_fail() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, userId);
        Node node = createNode(1L, createAttraction(101L, "대구수목원"));
        schedule.addNode(node);

        NodeArrangementRequest request = createArrangementRequest(
                List.of(
                        createDay(
                                LocalDate.of(2026, 5, 10),
                                List.of(
                                        createNodeArrangement(1L, 1),
                                        createNodeArrangement(1L, 2)
                                )
                        )
                )
        );

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> nodeService.updateNodeArrangement(userId, scheduleId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NODE_IN_REQUEST);
    }

    @Test
    @DisplayName("해당 스케줄에 속하지 않은 노드를 배치하려고 하면 예외가 발생한다")
    void updateNodeArrangement_nodeNotInSchedule_fail() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, userId);
        Node node = createNode(1L, createAttraction(101L, "대구수목원"));
        schedule.addNode(node);

        NodeArrangementRequest request = createArrangementRequest(
                List.of(
                        createDay(
                                LocalDate.of(2026, 5, 10),
                                List.of(createNodeArrangement(999L, 1))
                        )
                )
        );

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when & then
        assertThatThrownBy(() -> nodeService.updateNodeArrangement(userId, scheduleId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("노드를 삭제하면 같은 방문일자의 남은 노드 순서를 1부터 다시 정렬한다")
    void deleteNode_reorderAfterDelete_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;
        Long deleteNodeId = 2L;
        LocalDate visitDate = LocalDate.of(2026, 5, 10);

        Schedule schedule = createSchedule(scheduleId, userId);

        Node node1 = createNode(1L, createAttraction(101L, "대구수목원"));
        Node node2 = createNode(2L, createAttraction(102L, "김광석거리"));
        Node node3 = createNode(3L, createAttraction(103L, "서문시장"));

        schedule.addNode(node1);
        schedule.addNode(node2);
        schedule.addNode(node3);

        node1.updateVisitInfo(1, visitDate);
        node2.updateVisitInfo(2, visitDate);
        node3.updateVisitInfo(3, visitDate);

        when(scheduleRepository.findByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));
        when(nodeRepository.findByIdAndSchedule_Id(deleteNodeId, scheduleId))
                .thenReturn(Optional.of(node2));
        when(nodeRepository.findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(scheduleId, visitDate))
                .thenReturn(List.of(node1, node3));

        // when
        nodeService.deleteNode(userId, scheduleId, deleteNodeId);

        // then
        verify(nodeRepository).delete(node2);
        verify(nodeRepository).flush();

        assertThat(node1.getVisitOrder()).isEqualTo(1);
        assertThat(node3.getVisitOrder()).isEqualTo(2);
        assertThat(node1.getVisitDate()).isEqualTo(visitDate);
        assertThat(node3.getVisitDate()).isEqualTo(visitDate);
    }

    private Schedule createSchedule(Long scheduleId, Long userId) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);

        Schedule schedule = Schedule.create(
                user,
                "대구 여행",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11)
        );

        ReflectionTestUtils.setField(schedule, "id", scheduleId);
        return schedule;
    }

    private Attraction createAttraction(Long attractionId, String name) {
        Attraction attraction = mock(Attraction.class);

        lenient().when(attraction.getId()).thenReturn(attractionId);
        when(attraction.getContentTypeId()).thenReturn("12");
        when(attraction.getName()).thenReturn(name);
        when(attraction.getAddress()).thenReturn("대구광역시 테스트 주소");
        when(attraction.getLatitude()).thenReturn(BigDecimal.valueOf(35.8012345));
        when(attraction.getLongitude()).thenReturn(BigDecimal.valueOf(128.5123456));

        return attraction;
    }

    private Node createNode(Long nodeId, Attraction attraction) {
        Node node = Node.from(attraction);
        ReflectionTestUtils.setField(node, "id", nodeId);
        return node;
    }

    private NodeArrangementRequest createArrangementRequest(
            List<NodeArrangementRequest.DayArrangement> days
    ) {
        NodeArrangementRequest request = new NodeArrangementRequest();
        ReflectionTestUtils.setField(request, "days", days);
        return request;
    }

    private NodeArrangementRequest.DayArrangement createDay(
            LocalDate date,
            List<NodeArrangementRequest.NodeArrangement> nodes
    ) {
        NodeArrangementRequest.DayArrangement day =
                new NodeArrangementRequest.DayArrangement();

        ReflectionTestUtils.setField(day, "date", date);
        ReflectionTestUtils.setField(day, "nodes", nodes);

        return day;
    }

    private NodeArrangementRequest.NodeArrangement createNodeArrangement(
            Long nodeId,
            Integer visitOrder
    ) {
        NodeArrangementRequest.NodeArrangement nodeArrangement =
                new NodeArrangementRequest.NodeArrangement();

        ReflectionTestUtils.setField(nodeArrangement, "nodeId", nodeId);
        ReflectionTestUtils.setField(nodeArrangement, "visitOrder", visitOrder);

        return nodeArrangement;
    }
}