package com.ssafy.modu.domain.aicommand.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ToolExecutionContext {

    private Long userId;
    private Long scheduleId;
    private LocalDate date;
    private boolean apply;
}
