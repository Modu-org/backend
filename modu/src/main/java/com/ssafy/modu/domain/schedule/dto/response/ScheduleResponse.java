package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.schedule.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
// 스케줄 생성/수정 응답 DTO -> 스케줄의 기본 정보 반환
@Getter
@Builder
public class ScheduleResponse {

    private Long scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .title(schedule.getTitle())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .build();
    }
}
