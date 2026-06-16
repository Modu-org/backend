package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.routerecommend.dto.request.AutoArrangeRequest;
import com.ssafy.modu.domain.routerecommend.service.RouteRecommendService;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AutoArrangeUnscheduledNodesToolHandler implements ScheduleToolHandler {

    private final RouteRecommendService routeRecommendService;
    private final ScheduleToolSupport scheduleToolSupport;

    @Override
    public String getName() {
        return "auto_arrange_unscheduled_nodes";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "특정 일정의 미배치 노드를 전체 여행 날짜에 자동 배치하고, 동선 최소화와 관광지 타입 연속 회피를 고려해 방문 순서를 추천한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "scheduleId", Map.of("type", "integer", "description", "자동 배치할 일정 ID"),
                                        "apply", Map.of("type", "boolean", "description", "실제 반영 여부. 생략하면 true")
                                ),
                                "required", List.of("scheduleId")
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        Long scheduleId = scheduleToolSupport.resolveScheduleId(context, arguments);
        boolean toolApply = !arguments.has("apply") || arguments.path("apply").asBoolean(true);
        boolean shouldApply = context.isApply() && toolApply;

        if (!shouldApply) {
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("applied", false);
            preview.put("scheduleId", scheduleId);
            preview.put("message", "자동 배치 미리보기는 아직 지원하지 않습니다. apply=true로 요청하면 실제 자동 배치를 실행합니다.");

            return ToolExecutionResult.builder()
                    .toolName(getName())
                    .result(preview)
                    .build();
        }

        AutoArrangeRequest request = new AutoArrangeRequest();
        request.setDays(null);

        ScheduleDetailResponse schedule = routeRecommendService.autoArrange(
                scheduleId,
                context.getUserId(),
                request
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applied", true);
        result.put("scheduleId", scheduleId);
        result.put("message", "미배치 노드를 전체 날짜에 자동 배치했습니다.");

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(result)
                .schedule(schedule)
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_SCOPED);
    }
}
