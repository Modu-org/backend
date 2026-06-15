package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.node.entity.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GetDayNodesToolHandler implements ScheduleToolHandler {

    private final NodeToolSupport nodeToolSupport;

    @Override
    public String getName() {
        return "get_day_nodes";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "현재 일정의 특정 날짜에 배치된 노드 목록을 조회한다. 사용자의 '1번', '2번'은 visitOrder를 의미한다.",
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
        List<Node> nodes = nodeToolSupport.getDayNodes(
                context.getUserId(),
                context.getScheduleId(),
                context.getDate()
        );

        List<Map<String, Object>> nodeResults = nodes.stream()
                .map(nodeToolSupport::toNodeMap)
                .toList();

        Map<String, Object> result = Map.of(
                "date", context.getDate().toString(),
                "nodes", nodeResults
        );

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(result)
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_SCOPED);
    }
}
