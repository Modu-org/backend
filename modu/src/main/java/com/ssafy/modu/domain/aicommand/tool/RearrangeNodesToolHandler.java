package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import com.ssafy.modu.domain.node.dto.request.NodeArrangementRequest;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.service.NodeService;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RearrangeNodesToolHandler implements ScheduleToolHandler {

    private final NodeToolSupport nodeToolSupport;
    private final NodeService nodeService;

    @Override
    public String getName() {
        return "rearrange_nodes";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", "현재 날짜 안에서 노드 순서를 변경한다. sourceOrder/targetOrder는 DB nodeId가 아니라 화면에 보이는 visitOrder이다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "operation", Map.of(
                                                "type", "string",
                                                "enum", List.of("SWAP", "MOVE_BEFORE", "MOVE_AFTER", "MOVE_TO_FIRST", "MOVE_TO_LAST")
                                        ),
                                        "sourceOrder", Map.of("type", "integer"),
                                        "targetOrder", Map.of("type", "integer"),
                                        "sourceNodeId", Map.of("type", "integer"),
                                        "targetNodeId", Map.of("type", "integer"),
                                        "apply", Map.of("type", "boolean")
                                ),
                                "required", List.of("operation")
                        )
                )
        );
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments) {
        String operation = arguments.path("operation").asText(null);
        if (operation == null || operation.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        boolean toolApply = !arguments.has("apply") || arguments.path("apply").asBoolean(true);
        boolean shouldApply = context.isApply() && toolApply;

        List<Node> reordered = new ArrayList<>(nodeToolSupport.getDayNodes(
                context.getUserId(),
                context.getScheduleId(),
                context.getDate()
        ));

        if (reordered.isEmpty()) {
            throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
        }

        Node source = findSourceNode(reordered, arguments);
        Node target = findTargetNodeIfNeeded(reordered, arguments, operation);

        applyOperation(reordered, operation, source, target);

        List<Map<String, Object>> previewNodes = buildPreviewNodes(reordered);
        Map<String, Object> previewResult = Map.of(
                "applied", shouldApply,
                "date", context.getDate().toString(),
                "nodes", previewNodes
        );

        if (!shouldApply) {
            return ToolExecutionResult.builder()
                    .toolName(getName())
                    .result(previewResult)
                    .build();
        }

        NodeArrangementRequest request = buildArrangementRequest(context, reordered);
        ScheduleDetailResponse schedule = nodeService.updateNodeArrangement(
                context.getUserId(),
                context.getScheduleId(),
                request
        );

        return ToolExecutionResult.builder()
                .toolName(getName())
                .result(previewResult)
                .schedule(schedule)
                .build();
    }
    @Override
    public Set<AiCommandScope> getScopes() {
        return Set.of(AiCommandScope.SCHEDULE_SCOPED);
    }

    private Node findSourceNode(List<Node> nodes, JsonNode arguments) {
        Long sourceNodeId = arguments.hasNonNull("sourceNodeId") ? arguments.path("sourceNodeId").asLong() : null;
        Integer sourceOrder = arguments.hasNonNull("sourceOrder") ? arguments.path("sourceOrder").asInt() : null;

        return nodes.stream()
                .filter(node -> sourceNodeId != null && node.getId().equals(sourceNodeId)
                        || sourceOrder != null && node.getVisitOrder() != null && node.getVisitOrder().equals(sourceOrder))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
    }

    private Node findTargetNodeIfNeeded(List<Node> nodes, JsonNode arguments, String operation) {
        if ("MOVE_TO_FIRST".equals(operation) || "MOVE_TO_LAST".equals(operation)) {
            return null;
        }

        Long targetNodeId = arguments.hasNonNull("targetNodeId") ? arguments.path("targetNodeId").asLong() : null;
        Integer targetOrder = arguments.hasNonNull("targetOrder") ? arguments.path("targetOrder").asInt() : null;

        return nodes.stream()
                .filter(node -> targetNodeId != null && node.getId().equals(targetNodeId)
                        || targetOrder != null && node.getVisitOrder() != null && node.getVisitOrder().equals(targetOrder))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_NOT_FOUND));
    }

    private void applyOperation(List<Node> nodes, String operation, Node source, Node target) {
        switch (operation) {
            case "SWAP" -> {
                if (target == null) {
                    throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
                }
                int sourceIndex = nodes.indexOf(source);
                int targetIndex = nodes.indexOf(target);
                nodes.set(sourceIndex, target);
                nodes.set(targetIndex, source);
            }
            case "MOVE_TO_FIRST" -> {
                nodes.remove(source);
                nodes.add(0, source);
            }
            case "MOVE_TO_LAST" -> {
                nodes.remove(source);
                nodes.add(source);
            }
            case "MOVE_BEFORE" -> moveAroundTarget(nodes, source, target, false);
            case "MOVE_AFTER" -> moveAroundTarget(nodes, source, target, true);
            default -> throw new BusinessException(ErrorCode.UNSUPPORTED_AI_TOOL_OPERATION);
        }
    }

    private void moveAroundTarget(List<Node> nodes, Node source, Node target, boolean after) {
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_TOOL_ARGUMENTS);
        }

        if (source.equals(target)) {
            return;
        }

        nodes.remove(source);
        int targetIndex = nodes.indexOf(target);
        nodes.add(after ? targetIndex + 1 : targetIndex, source);
    }

    private List<Map<String, Object>> buildPreviewNodes(List<Node> reordered) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < reordered.size(); i++) {
            Node node = reordered.get(i);
            results.add(Map.of(
                    "nodeId", node.getId(),
                    "visitOrder", i + 1,
                    "name", node.getAttraction().getName(),
                    "contentTypeId", node.getAttraction().getContentTypeId()
            ));
        }

        return results;
    }

    private NodeArrangementRequest buildArrangementRequest(ToolExecutionContext context, List<Node> reordered) {
        List<NodeArrangementRequest.NodeArrangement> nodeArrangements = new ArrayList<>();

        for (int i = 0; i < reordered.size(); i++) {
            nodeArrangements.add(NodeArrangementRequest.NodeArrangement.of(
                    reordered.get(i).getId(),
                    i + 1
            ));
        }

        NodeArrangementRequest.DayArrangement dayArrangement = NodeArrangementRequest.DayArrangement.of(
                context.getDate(),
                nodeArrangements
        );

        return NodeArrangementRequest.of(List.of(dayArrangement));
    }
}
