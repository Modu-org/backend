package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GetUserSchedulesToolHandler implements ScheduleToolHandler {

    private final ScheduleService scheduleService;
    private final ScheduleToolSupport scheduleToolSupport;

    @Override
    public String getName() {
        return "get_user_schedules";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "현재 사용자의 여행 일정 목록을 조회한다. 사용자가 일정 이름을 말했거나 기존 일정 중 하나를 선택해야 할 때 사용한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(),
                                "required", List.of()
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        List<Map<String, Object>> schedules = scheduleService.getSchedules(context.getUserId()).stream()
                .map(scheduleToolSupport::toScheduleMap)
                .toList();

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(Map.of("schedules", schedules))
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_WORKFLOW);
    }
}
