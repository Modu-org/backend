package com.ssafy.modu.Schedule.service;

import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleCreateRequest;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleResponse;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("스케줄 생성에 성공한다")
    void createSchedule_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        ScheduleCreateRequest request = createScheduleCreateRequest(
                "대구 1박 2일 여행",
                "대구광역시",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                3,
                150000
        );

        when(userRepository.findByIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(user));

        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> {
                    Schedule savedSchedule = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedSchedule, "id", 10L);
                    return savedSchedule;
                });

        // when
        ScheduleResponse response = scheduleService.createSchedule(userId, request);

        // then
        assertThat(response.getScheduleId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("대구 1박 2일 여행");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 11));

        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 생성 시 제목이 비어 있으면 제목 없음으로 저장된다")
    void createSchedule_blankTitle_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        ScheduleCreateRequest request = createScheduleCreateRequest(
                "   ",
                "대구광역시",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                3,
                150000
        );

        when(userRepository.findByIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(user));

        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> {
                    Schedule savedSchedule = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedSchedule, "id", 10L);
                    return savedSchedule;
                });

        // when
        ScheduleResponse response = scheduleService.createSchedule(userId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("제목 없음");
    }

    @Test
    @DisplayName("스케줄 생성 시 시작일이 종료일보다 늦으면 예외가 발생한다")
    void createSchedule_invalidTripDate_fail() {
        // given
        Long userId = 1L;

        ScheduleCreateRequest request = createScheduleCreateRequest(
                "대구 여행",
                "대구광역시",
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 10),
                3,
                150000
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.createSchedule(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRIP_DATE);

        verify(userRepository, never()).findByIdAndIsDeletedFalse(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 생성 시 인원 수가 1명 미만이면 예외가 발생한다")
    void createSchedule_invalidPeopleCount_fail() {
        // given
        Long userId = 1L;

        ScheduleCreateRequest request = createScheduleCreateRequest(
                "대구 여행",
                "대구광역시",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                0,
                150000
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.createSchedule(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PEOPLE_COUNT);

        verify(userRepository, never()).findByIdAndIsDeletedFalse(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 생성 시 예산이 음수이면 예외가 발생한다")
    void createSchedule_invalidBudget_fail() {
        // given
        Long userId = 1L;

        ScheduleCreateRequest request = createScheduleCreateRequest(
                "대구 여행",
                "대구광역시",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                3,
                -1000
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.createSchedule(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BUDGET);

        verify(userRepository, never()).findByIdAndIsDeletedFalse(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 목록 조회에 성공한다")
    void getSchedules_success() {
        // given
        Long userId = 1L;

        Schedule schedule1 = createSchedule(10L, createUser(userId), "대구 여행");
        Schedule schedule2 = createSchedule(11L, createUser(userId), "부산 여행");

        when(scheduleRepository.findByUser_IdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(schedule1, schedule2));

        when(nodeRepository.countBySchedule_Id(10L)).thenReturn(3L);
        when(nodeRepository.countBySchedule_Id(11L)).thenReturn(5L);

        // when
        List<ScheduleSummaryResponse> responses = scheduleService.getSchedules(userId);

        // then
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).getScheduleId()).isEqualTo(10L);
        assertThat(responses.get(0).getTitle()).isEqualTo("대구 여행");
        assertThat(responses.get(0).getNodeCount()).isEqualTo(3L);

        assertThat(responses.get(1).getScheduleId()).isEqualTo(11L);
        assertThat(responses.get(1).getTitle()).isEqualTo("부산 여행");
        assertThat(responses.get(1).getNodeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("스케줄 상세 조회에 성공한다")
    void getScheduleDetail_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, createUser(userId), "대구 여행");

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when
        ScheduleDetailResponse response = scheduleService.getScheduleDetail(userId, scheduleId);

        // then
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getTitle()).isEqualTo("대구 여행");
        assertThat(response.getDays()).hasSize(2);
        assertThat(response.getUnscheduledNodes()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 스케줄 상세 조회 시 예외가 발생한다")
    void getScheduleDetail_scheduleNotFound_fail() {
        // given
        Long userId = 1L;
        Long scheduleId = 999L;

        when(scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.getScheduleDetail(userId, scheduleId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("스케줄 수정에 성공한다")
    void updateSchedule_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, createUser(userId), "대구 여행");

        ScheduleUpdateRequest request = createScheduleUpdateRequest(
                "대구 가족 여행",
                "대구광역시",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                4,
                200000
        );

        when(scheduleRepository.findByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when
        ScheduleResponse response = scheduleService.updateSchedule(userId, scheduleId, request);

        // then
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getTitle()).isEqualTo("대구 가족 여행");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }

    @Test
    @DisplayName("스케줄 삭제에 성공한다")
    void deleteSchedule_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, createUser(userId), "대구 여행");

        when(scheduleRepository.findByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        // when
        scheduleService.deleteSchedule(userId, scheduleId);

        // then
        verify(scheduleRepository).delete(schedule);
    }

    @Test
    @DisplayName("스케줄 요약 조회에 성공한다")
    void getScheduleSummary_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 10L;

        Schedule schedule = createSchedule(scheduleId, createUser(userId), "대구 여행");

        when(scheduleRepository.findByIdAndUser_Id(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        when(nodeRepository.countBySchedule_Id(scheduleId))
                .thenReturn(4L);

        // when
        ScheduleSummaryResponse response = scheduleService.getScheduleSummary(userId, scheduleId);

        // then
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getTitle()).isEqualTo("대구 여행");
        assertThat(response.getNodeCount()).isEqualTo(4L);
    }

    private User createUser(Long userId) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        return user;
    }

    private Schedule createSchedule(Long scheduleId, User user, String title) {
        Schedule schedule = Schedule.create(
                user,
                title,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11)
        );

        ReflectionTestUtils.setField(schedule, "id", scheduleId);
        return schedule;
    }

    private ScheduleCreateRequest createScheduleCreateRequest(
            String title,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            Integer peopleCount,
            Integer budget
    ) {
        ScheduleCreateRequest request = new ScheduleCreateRequest();

        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "region", region);
        ReflectionTestUtils.setField(request, "startDate", startDate);
        ReflectionTestUtils.setField(request, "endDate", endDate);
        ReflectionTestUtils.setField(request, "peopleCount", peopleCount);
        ReflectionTestUtils.setField(request, "budget", budget);

        return request;
    }

    private ScheduleUpdateRequest createScheduleUpdateRequest(
            String title,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            Integer peopleCount,
            Integer budget
    ) {
        ScheduleUpdateRequest request = new ScheduleUpdateRequest();

        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "region", region);
        ReflectionTestUtils.setField(request, "startDate", startDate);
        ReflectionTestUtils.setField(request, "endDate", endDate);
        ReflectionTestUtils.setField(request, "peopleCount", peopleCount);
        ReflectionTestUtils.setField(request, "budget", budget);

        return request;
    }
}