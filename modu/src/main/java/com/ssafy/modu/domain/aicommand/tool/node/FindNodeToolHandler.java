package com.ssafy.modu.domain.aicommand.tool.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.support.NodeToolSupport;
import com.ssafy.modu.domain.aicommand.tool.schedule.ScheduleToolHandler;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FindNodeToolHandler implements ScheduleToolHandler {

    private final NodeToolSupport nodeToolSupport;

    @Override
    public String getName() {
        return "find_node";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "현재 날짜의 노드 중 visitOrder 또는 관광지 이름으로 노드를 찾는다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "order", Map.of("type", "integer", "description", "화면에 보이는 방문 순서. 예: 3번이면 3"),
                                        "name", Map.of("type", "string", "description", "관광지 이름 또는 이름 일부")
                                ),
                                "required", List.of()
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        Integer order = arguments.hasNonNull("order") ? arguments.path("order").asInt() : null;
        String name = arguments.hasNonNull("name") ? arguments.path("name").asText() : null;

        List<Node> nodes = nodeToolSupport.getDayNodes(
                context.getUserId(),
                context.getScheduleId(),
                context.getDate()
        );

        Node found = null;

        if (order != null) {
            found = nodes.stream()
                    .filter(node -> node.getVisitOrder() != null && node.getVisitOrder().equals(order))
                    .findFirst()
                    .orElse(null);
        }

        if (found == null && name != null && !name.isBlank()) {
            String normalizedName = normalize(name);
            found = nodes.stream()
                    .filter(node -> normalize(node.getAttraction().getName()).contains(normalizedName))
                    .findFirst()
                    .orElse(null);
        }

        if (found == null) {
            throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
        }

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(nodeToolSupport.toNodeMap(found))
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_SCOPED);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }
}
