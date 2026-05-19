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
    private String region;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer peopleCount;
    private Integer budget;
    private long nodeCount;

    public static ScheduleSummaryResponse of(Schedule schedule, long nodeCount) {
        return ScheduleSummaryResponse.builder()
                .scheduleId(schedule.getId())
                .title(schedule.getTitle())
                .region(schedule.getRegion())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .peopleCount(schedule.getPeopleCount())
                .budget(schedule.getBudget())
                .nodeCount(nodeCount)
                .build();
    }
}
