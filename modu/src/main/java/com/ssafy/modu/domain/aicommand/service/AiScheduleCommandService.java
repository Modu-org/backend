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
        List<String> executedTools = new ArrayList<>();
        ScheduleDetailResponse latestSchedule = null;
        String latestMessage = null;

        // 여러 차례에 걸쳐서 동작이 진행될 수 있기 때문에 round를 두고, ai와 여러번 대화를 거침
        // 대화를 하면서, 점점 더 요구사항이 구체화됨
        for (int round = 0; round < MAX_TOOL_CALL_ROUND; round++) {
            ToolChatResponse aiResponse = gmsOpenAIClient.chatWithTools(messages, tools);

            // 더 이상 받아올 응답이 없는 상황에서는 일이 다 처리되었다고 가정
            if (!aiResponse.hasToolCalls()) {
                latestMessage = aiResponse.getContent();
                return AiScheduleCommandResponse.builder()
                        .message(latestMessage == null || latestMessage.isBlank()
                                ? "일정 명령 처리가 완료되었습니다."
                                : latestMessage)
                        // db에 적용된 일정
                        .schedule(latestSchedule)
                        // 제일 마지막으로 실행된 tool
                        .executedTools(executedTools)
                        .build();
            }

            messages.add(objectMapper.convertValue(aiResponse.getAssistantMessage(), Map.class));

            for (ToolCall toolCall : aiResponse.getToolCalls()) {
                // 실제로 백엔드 로직 실행
                ToolExecutionResult toolResult = scheduleToolExecutor.execute(
                        context,
                        toolCall.getName(),
                        toolCall.getArguments()
                );

                executedTools.add(toolCall.getName());

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
