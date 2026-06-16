package com.ssafy.modu.domain.aicommand.tool.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.aicommand.tool.support.ScheduleToolSupport;
import com.ssafy.modu.domain.node.dto.request.NodeCreateRequest;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.service.NodeService;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AddAttractionToScheduleToolHandler implements ScheduleToolHandler {

    private final NodeService nodeService;
    private final ScheduleToolSupport scheduleToolSupport;

    @Override
    public String getName() {
        return "add_attraction_to_schedule";
    }

    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_WORKFLOW);
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "프론트가 선택해서 context로 전달한 단일 관광지 ID를 특정 일정에 미배치 노드로 추가한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "scheduleId", Map.of("type", "integer", "description", "관광지를 추가할 일정 ID"),
                                        "attractionId", Map.of("type", "integer", "description", "추가할 관광지 ID. 반드시 context의 선택된 관광지 ID와 같아야 함")
                                ),
                                "required", List.of("scheduleId")
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        Long scheduleId = scheduleToolSupport.resolveScheduleId(context, arguments);
        Long attractionId = resolveAttractionId(context, arguments);

        NodeCreateRequest request = new NodeCreateRequest();
        request.setAttractionId(attractionId);

        NodeResponse node = nodeService.addNode(context.getUserId(), scheduleId, request);

        Map<String, Object> addedNode = new LinkedHashMap<>();
        addedNode.put("nodeId", node.getNodeId());
        addedNode.put("scheduleId", node.getScheduleId());
        addedNode.put("attractionId", node.getAttractionId());
        addedNode.put("visitOrder", node.getVisitOrder());
        addedNode.put("visitDate", node.getVisitDate());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduleId", scheduleId);
        result.put("addedNode", addedNode);

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(result)
                .build();
    }

    private Long resolveAttractionId(ToolExecutionContext context, JsonNode arguments) {
        Long contextAttractionId = context.getAttractionId();

        if (contextAttractionId == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        Long argumentAttractionId = arguments.hasNonNull("attractionId")
                ? arguments.path("attractionId").asLong()
                : contextAttractionId;

        if (!contextAttractionId.equals(argumentAttractionId)) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        return contextAttractionId;
    }
}
