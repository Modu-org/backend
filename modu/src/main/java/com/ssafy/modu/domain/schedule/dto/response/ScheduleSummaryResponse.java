package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.schedule.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
// 스케줄 요약 정보를 반환한다.
@Getter
@Builder
public class ScheduleSummaryResponse {

    private Long scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private long nodeCount;
    private boolean arrivalShared;

    public static ScheduleSummaryResponse of(Schedule schedule, long nodeCount) {
        return ScheduleSummaryResponse.builder()
                .scheduleId(schedule.getId())
                .title(schedule.getTitle())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .nodeCount(nodeCount)
                .arrivalShared(schedule.isArrivalShared())
                .build();
    }
}
