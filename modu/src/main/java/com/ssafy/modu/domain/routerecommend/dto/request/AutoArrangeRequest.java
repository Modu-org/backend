package com.ssafy.modu.domain.routerecommend.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AutoArrangeRequest {

    private List<DayCondition> days;

    @Getter
    @Setter
    public static class DayCondition {
        private LocalDate date;
        private Long startNodeId;
        private Long endNodeId;
    }
}