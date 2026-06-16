package com.ssafy.modu.domain.aicommand.tool.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.support.ScheduleToolSupport;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FindScheduleToolHandler implements ScheduleToolHandler {

    private final ScheduleService scheduleService;
    private final ScheduleToolSupport scheduleToolSupport;

    @Override
    public String getName() {
        return "find_schedule";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "사용자의 일정 목록에서 제목 또는 날짜 조건으로 일정을 찾는다. 일정명이 모호하면 날짜 조건을 함께 사용한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of(
                                                "type", "string",
                                                "description", "찾을 일정 제목 또는 제목 일부"
                                        ),
                                        "startDate", Map.of(
                                                "type", "string",
                                                "description", "일정 시작일. yyyy-MM-dd 형식"
                                        ),
                                        "endDate", Map.of(
                                                "type", "string",
                                                "description", "일정 종료일. yyyy-MM-dd 형식"
                                        ),
                                        "containsDate", Map.of(
                                                "type", "string",
                                                "description", "해당 날짜를 포함하는 일정. yyyy-MM-dd 형식"
                                        )
                                ),
                                "required", List.of()
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        String title = arguments.hasNonNull("title")
                ? arguments.path("title").asText()
                : null;

        LocalDate startDate = parseOptionalDate(arguments, "startDate");
        LocalDate endDate = parseOptionalDate(arguments, "endDate");
        LocalDate containsDate = parseOptionalDate(arguments, "containsDate");

        if (isBlank(title) && startDate == null && endDate == null && containsDate == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        List<ScheduleSummaryResponse> matched = scheduleService.getSchedules(context.getUserId()).stream()
                .filter(schedule -> matchesTitle(schedule, title))
                .filter(schedule -> matchesStartDate(schedule, startDate))
                .filter(schedule -> matchesEndDate(schedule, endDate))
                .filter(schedule -> matchesContainsDate(schedule, containsDate))
                .toList();

        List<Map<String, Object>> candidates = matched.stream()
                .map(scheduleToolSupport::toScheduleMap)
                .toList();

        if (matched.size() == 1) {
            return ToolExecutionResult.builder()
                    .toolName(getName())
                    .result(Map.of(
                            "matched", true,
                            "needUserSelection", false,
                            "schedule", candidates.get(0)
                    ))
                    .build();
        }

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(Map.of(
                        "matched", false,
                        "needUserSelection", matched.size() > 1,
                        "candidates", candidates
                ))
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_WORKFLOW);
    }
    private boolean matchesTitle(ScheduleSummaryResponse schedule, String title) {
        if (title == null || title.isBlank()) {
            return true;
        }

        return scheduleToolSupport.normalize(schedule.getTitle())
                .contains(scheduleToolSupport.normalize(title));
    }

    private boolean matchesStartDate(ScheduleSummaryResponse schedule, LocalDate startDate) {
        return startDate == null || schedule.getStartDate().equals(startDate);
    }

    private boolean matchesEndDate(ScheduleSummaryResponse schedule, LocalDate endDate) {
        return endDate == null || schedule.getEndDate().equals(endDate);
    }

    private boolean matchesContainsDate(ScheduleSummaryResponse schedule, LocalDate containsDate) {
        if (containsDate == null) {
            return true;
        }

        return !containsDate.isBefore(schedule.getStartDate())
                && !containsDate.isAfter(schedule.getEndDate());
    }
    private LocalDate parseOptionalDate(JsonNode arguments, String fieldName) {
        if (!arguments.hasNonNull(fieldName)) {
            return null;
        }

        try {
            return LocalDate.parse(arguments.path(fieldName).asText());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
