package com.ssafy.modu.domain.schedule.service;

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
    // 스케줄 생성일 기준으로 정렬해서 보여줌
    public List<ScheduleSummaryResponse> getSchedules(Long userId) {
        return scheduleRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(schedule -> ScheduleSummaryResponse.of(
                        schedule,
                        nodeRepository.countBySchedule_Id(schedule.getId())
                ))
                .toList();
    }

    // 스케줄(노드포함)가져옴
    public ScheduleDetailResponse getScheduleDetail(Long userId, Long scheduleId) {
        Schedule schedule = getScheduleWithNodes(userId, scheduleId);
        return ScheduleDetailResponse.from(schedule);
    }

    // 스케줄 수정
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

    // 스케줄 삭제
    @Transactional
    public void deleteSchedule(Long userId, Long scheduleId) {
        Schedule schedule = getSchedule(userId, scheduleId);
        scheduleRepository.delete(schedule);
    }

    // 스케줄 요약 정보 가져옴
    public ScheduleSummaryResponse getScheduleSummary(Long userId, Long scheduleId) {
        Schedule schedule = getSchedule(userId, scheduleId);
        long nodeCount = nodeRepository.countBySchedule_Id(scheduleId);
        return ScheduleSummaryResponse.of(schedule, nodeCount);
    }

    // 단순히 스케줄 정보만 가져옴
    private Schedule getSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    // 스케줄 안에 있는 노드 정보들끼지 가져옴
    private Schedule getScheduleWithNodes(Long userId, Long scheduleId) {
        return scheduleRepository.findWithNodesByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateScheduleRequest(LocalDate startDate, LocalDate endDate) {
        // 여행일자 정합성 (둘 중에 하나가 null이거나, 시작일보다 종료일이 앞서거나)
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_TRIP_DATE);
        }
    }
}
