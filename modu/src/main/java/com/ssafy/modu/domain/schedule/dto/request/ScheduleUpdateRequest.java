package com.ssafy.modu.domain.schedule.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ScheduleUpdateRequest {

    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}
