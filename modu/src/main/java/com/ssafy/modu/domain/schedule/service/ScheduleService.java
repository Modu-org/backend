package com.ssafy.modu.domain.schedule.service;

import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.edge.service.EdgeService;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleCreateRequest;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleResponse;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final NodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final EdgeService edgeService;

    @Transactional
    public ScheduleResponse createSchedule(Long userId, ScheduleCreateRequest request) {
        validateScheduleRequest(request.getStartDate(), request.getEndDate());

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Schedule schedule = Schedule.create(
                user,
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public List<ScheduleSummaryResponse> getSchedules(Long userId) {
        return scheduleRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(schedule -> ScheduleSummaryResponse.of(
                        schedule,
                        nodeRepository.countBySchedule_Id(schedule.getId())
                ))
                .toList();
    }

    public ScheduleDetailResponse getScheduleDetail(Long userId, Long scheduleId) {
        Schedule schedule = getScheduleWithNodes(userId, scheduleId);

        List<Edge> edges = edgeService.getEdgesByScheduleId(scheduleId);

        return ScheduleDetailResponse.from(schedule, edges);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long userId, Long scheduleId, ScheduleUpdateRequest request) {
        validateScheduleRequest(request.getStartDate(), request.getEndDate());

        Schedule schedule = getSchedule(userId, scheduleId);
        schedule.update(
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long userId, Long scheduleId) {
        Schedule schedule = getSchedule(userId, scheduleId);

        edgeService.deleteEdgesByScheduleId(scheduleId);

        scheduleRepository.delete(schedule);
    }

    public ScheduleSummaryResponse getScheduleSummary(Long userId, Long scheduleId) {
        Schedule schedule = getSchedule(userId, scheduleId);
        long nodeCount = nodeRepository.countBySchedule_Id(scheduleId);
        return ScheduleSummaryResponse.of(schedule, nodeCount);
    }
    @Transactional
    public void updateArrivalShared(
            Long userId,
            Long scheduleId,
            boolean enabled
    ) {
        Schedule schedule = getSchedule(userId, scheduleId);
        schedule.updateArrivalShared(enabled);
    }

    private Schedule getSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Schedule getScheduleWithNodes(Long userId, Long scheduleId) {
        return scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateScheduleRequest(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_TRIP_DATE);
        }
    }
}