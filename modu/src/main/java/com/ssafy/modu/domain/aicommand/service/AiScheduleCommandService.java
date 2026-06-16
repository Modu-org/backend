package com.ssafy.modu.domain.aicommand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.domain.aicommand.dto.request.AiScheduleCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.response.AiScheduleCommandResponse;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.external.ai.client.GmsOpenAIClient;
import com.ssafy.modu.external.ai.dto.tool.ToolCall;
import com.ssafy.modu.external.ai.dto.tool.ToolChatResponse;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiScheduleCommandService {

    private static final int MAX_TOOL_CALL_ROUND = 5;

    private final GmsOpenAIClient gmsOpenAIClient;
    private final ScheduleToolRegistry scheduleToolRegistry;
    private final ScheduleToolExecutor scheduleToolExecutor;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiScheduleCommandResponse handleCommand(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request
    ) {
        try {
            return processCommand(userId, scheduleId, request);
        } catch (BusinessException e) {
            rollbackIfPossible();

            return AiScheduleCommandResponse.of(
                    toFriendlyMessage(e.getErrorCode()),
                    null
            );
        } catch (Exception e) {
            rollbackIfPossible();

            return AiScheduleCommandResponse.of(
                    "죄송해요. 요청을 처리하는 중에 잠시 문제가 발생했어요. 다시 한 번 말씀해 주세요.",
                    null
            );
        }
    }

    private AiScheduleCommandResponse processCommand(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request
    ) {
        ToolExecutionContext context = new ToolExecutionContext(
                userId,
                scheduleId,
                request.getDate(),
                request.isApply()
        );

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "developer",
                "content", buildDeveloperPrompt()
        ));
        messages.add(Map.of(
                "role", "user",
                "content", buildUserPrompt(request)
        ));

        // ScheduleToolHandler의 구현체들의 getDefinition들을 다 가지고 옴
        // 노드 순서를 이동하기 위한 백엔드 메서드들의 함수 정보들을 가지고 옴
        List<Map<String, Object>> tools = scheduleToolRegistry.getDefinitions();

        ScheduleDetailResponse latestSchedule = null;

        // 여러 차례에 걸쳐서 동작이 진행될 수 있기 때문에 round를 두고, ai와 여러번 대화를 거침
        // 대화를 하면서, 점점 더 요구사항이 구체화됨
        for (int round = 0; round < MAX_TOOL_CALL_ROUND; round++) {
            ToolChatResponse aiResponse = gmsOpenAIClient.chatWithTools(messages, tools);

            // 더 이상 받아올 응답이 없는 상황에서는 일이 다 처리되었다고 가정
            if (!aiResponse.hasToolCalls()) {
                String latestMessage = aiResponse.getContent();

                String assistantMessage = latestMessage == null || latestMessage.isBlank()
                        ? "일정 명령 처리가 완료되었습니다."
                        : latestMessage;

                return AiScheduleCommandResponse.of(
                        assistantMessage,
                        latestSchedule
                );
            }

            messages.add(objectMapper.convertValue(aiResponse.getAssistantMessage(), Map.class));

            for (ToolCall toolCall : aiResponse.getToolCalls()) {
                // 실제로 백엔드 로직 실행
                ToolExecutionResult toolResult = scheduleToolExecutor.execute(
                        context,
                        toolCall.getName(),
                        toolCall.getArguments()
                );

                if (toolResult.getSchedule() != null) {
                    latestSchedule = toolResult.getSchedule();
                }

                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCall.getId(),
                        "content", toJson(toolResult.getResult())
                ));
            }
        }

        throw new BusinessException(ErrorCode.AI_TOOL_CALL_LIMIT_EXCEEDED);
    }

    private void rollbackIfPossible() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception ignored) {
            // 트랜잭션이 없는 상황이면 롤백 처리 없이 사용자 친화 응답만 반환한다.
        }
    }

    private String toFriendlyMessage(ErrorCode errorCode) {
        return switch (errorCode) {
            case NODE_NOT_FOUND ->
                    "말씀하신 장소나 순서를 찾지 못했어요. 화면에 보이는 번호나 장소 이름을 다시 확인해 주세요.";

            case INVALID_NODE_VISIT_DATE ->
                    "선택한 날짜가 이 일정의 여행 기간에 포함되어 있지 않아요. 일정에 포함된 날짜에서 다시 요청해 주세요.";

            case SCHEDULE_NOT_FOUND ->
                    "해당 일정을 찾을 수 없어요. 일정이 삭제되었거나 접근할 수 없는 일정일 수 있어요.";

            case INVALID_AI_TOOL_ARGUMENTS ->
                    "요청하신 내용을 정확히 이해하지 못했어요. 예를 들어 '3번과 4번 바꿔줘'처럼 다시 말씀해 주세요.";

            case UNSUPPORTED_AI_TOOL ->
                    "아직 지원하지 않는 일정 명령이에요. 순서 변경이나 장소 이동처럼 다시 요청해 주세요.";

            case UNSUPPORTED_AI_TOOL_OPERATION ->
                    "아직 지원하지 않는 일정 변경 방식이에요. '3번을 맨 앞으로 보내줘'처럼 다시 요청해 주세요.";

            case AI_TOOL_CALL_LIMIT_EXCEEDED ->
                    "요청을 처리하려고 했지만 명령이 조금 복잡했어요. 한 번에 하나의 변경만 요청해 주세요.";

            case AI_API_ERROR ->
                    "AI 응답을 처리하는 중에 문제가 발생했어요. 잠시 후 다시 시도해 주세요.";

            default ->
                    "요청을 처리하지 못했어요. 문장을 조금 더 구체적으로 다시 말씀해 주세요.";
        };
    }

    // 일정 수정 tool을 위한 페르소나 프롬프트 작성
    private String buildDeveloperPrompt() {
        return """
                너는 여행 일정 편집 도우미다.
                사용자의 자연어 명령을 해석해서 반드시 제공된 tools 중 필요한 tool을 호출해야 한다.
                일정 ID와 날짜는 서버가 이미 검증한 context로 전달하므로 사용자가 말하지 않아도 된다.
                사용자의 '1번', '2번', '3번'은 DB nodeId가 아니라 화면에 보이는 visitOrder이다.
                단순 순서 변경은 rearrange_nodes를 호출한다.
                장소 이름 기반 명령은 필요하면 find_node로 nodeId를 찾은 뒤 rearrange_nodes를 호출한다.
                DB에 없는 nodeId를 만들거나 새로운 노드를 추가하지 마라.
                최종 응답은 한국어로 짧게 작성한다.
                """;
    }

    // 사용자에 따라서 달라지는 프롬프트 작성
    private String buildUserPrompt(AiScheduleCommandRequest request) {
        return """
                대상 날짜: %s
                실제 반영 여부: %s
                사용자 명령: %s
                """.formatted(
                request.getDate(),
                request.isApply(),
                request.getText()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }
}