package com.ssafy.modu.domain.aicommand.tool.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleCreateRequest;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleResponse;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CreateScheduleToolHandler implements ScheduleToolHandler {

    private final ScheduleService scheduleService;

    @Override
    public String getName() {
        return "create_schedule";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "새 여행 일정을 생성한다. title, startDate, endDate가 모두 확정된 경우에만 호출한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of("type", "string", "description", "새 일정 제목"),
                                        "startDate", Map.of("type", "string", "description", "여행 시작일. yyyy-MM-dd 형식"),
                                        "endDate", Map.of("type", "string", "description", "여행 종료일. yyyy-MM-dd 형식")
                                ),
                                "required", List.of("title", "startDate", "endDate")
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        String title = arguments.hasNonNull("title") ? arguments.path("title").asText() : null;
        LocalDate startDate = parseDate(arguments, "startDate");
        LocalDate endDate = parseDate(arguments, "endDate");

        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setTitle(title);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        ScheduleResponse schedule = scheduleService.createSchedule(context.getUserId(), request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduleId", schedule.getScheduleId());
        result.put("title", schedule.getTitle());
        result.put("startDate", schedule.getStartDate());
        result.put("endDate", schedule.getEndDate());

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(result)
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_WORKFLOW);
    }

    private LocalDate parseDate(JsonNode arguments, String fieldName) {
        if (!arguments.hasNonNull(fieldName)) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        try {
            return LocalDate.parse(arguments.path(fieldName).asText());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }
    }
}
