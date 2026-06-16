package com.ssafy.modu.domain.aicommand.tool.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ScheduleToolSupport {

    public Long resolveScheduleId(ToolExecutionContext context, JsonNode arguments) {
        Long argumentScheduleId = arguments.hasNonNull("scheduleId")
                ? arguments.path("scheduleId").asLong()
                : null;

        Long contextScheduleId = context.getScheduleId();

        if (contextScheduleId != null && argumentScheduleId != null && !contextScheduleId.equals(argumentScheduleId)) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        Long scheduleId = contextScheduleId != null ? contextScheduleId : argumentScheduleId;

        if (scheduleId == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        return scheduleId;
    }

    public Map<String, Object> toScheduleMap(ScheduleSummaryResponse schedule) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduleId", schedule.getScheduleId());
        result.put("title", schedule.getTitle());
        result.put("startDate", schedule.getStartDate());
        result.put("endDate", schedule.getEndDate());
        result.put("nodeCount", schedule.getNodeCount());
        return result;
    }

    public String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }

}