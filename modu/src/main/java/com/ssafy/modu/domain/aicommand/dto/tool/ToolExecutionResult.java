package com.ssafy.modu.domain.aicommand.dto.tool;

import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ToolExecutionResult {

    private String toolName;
    private Object result;
    private ScheduleDetailResponse schedule;
}
