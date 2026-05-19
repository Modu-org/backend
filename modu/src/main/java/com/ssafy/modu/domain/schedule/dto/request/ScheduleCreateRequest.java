package com.ssafy.modu.domain.schedule.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ScheduleCreateRequest {

    private String title;
    private String region;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer peopleCount;
    private Integer budget;
}
