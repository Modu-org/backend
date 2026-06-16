package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GetScheduleDetailToolHandler implements ScheduleToolHandler {

    private final ScheduleService scheduleService;
    private final ScheduleToolSupport scheduleToolSupport;

    @Override
    public String getName() {
        return "get_schedule_detail";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "특정 일정의 상세 정보를 조회한다. 최종 응답에 최신 schedule을 포함해야 할 때 사용한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "scheduleId", Map.of("type", "integer", "description", "조회할 일정 ID")
                                ),
                                "required", List.of("scheduleId")
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        Long scheduleId = scheduleToolSupport.resolveScheduleId(context, arguments);
        ScheduleDetailResponse schedule = scheduleService.getScheduleDetail(context.getUserId(), scheduleId);

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(Map.of("scheduleId", scheduleId))
                .schedule(schedule)
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(
                AiCommandScope.SCHEDULE_SCOPED,
                AiCommandScope.SCHEDULE_WORKFLOW
        );
    }
}
