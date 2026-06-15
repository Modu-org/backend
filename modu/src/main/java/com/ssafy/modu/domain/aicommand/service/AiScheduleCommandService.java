package com.ssafy.modu.domain.aicommand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.domain.aicommand.dto.request.AiScheduleCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.response.AiScheduleCommandResponse;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
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

    private static final int MAX_TOOL_CALL_ROUND = 10;

    private final GmsOpenAIClient gmsOpenAIClient;
    private final ScheduleToolRegistry scheduleToolRegistry;
    private final ScheduleToolExecutor scheduleToolExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 특정 일정 화면 내부에서 실행되는 AI 명령.
     * 예:
     * - 3번이랑 4번 바꿔줘
     * - 3번을 제일 먼저 보내줘
     * - 이 관광지를 현재 일정에 추가해줘
     * - 미배치된 장소들 자동 배치해줘
     */
    @Transactional
    public AiScheduleCommandResponse handleScheduleScopedCommand(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request
    ) {
        return handleCommand(
                userId,
                scheduleId,
                request,
                AiCommandScope.SCHEDULE_SCOPED
        );
    }

    /**
     * 홈/관광지 목록/관광지 상세처럼 아직 특정 일정이 확정되지 않은 화면에서 실행되는 AI 명령.
     * 예:
     * - 서울1 일정에 이 관광지 추가해줘
     * - 6월 20일부터 22일까지 새 일정 만들어서 이 관광지 추가해줘
     * - 서울1 일정에 추가하고 자동 배치해줘
     */
    @Transactional
    public AiScheduleCommandResponse handleScheduleWorkflowCommand(
            Long userId,
            AiScheduleCommandRequest request
    ) {
        return handleCommand(
                userId,
                null,
                request,
                AiCommandScope.SCHEDULE_WORKFLOW
        );
    }

    private AiScheduleCommandResponse handleCommand(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request,
            AiCommandScope scope
    ) {
        ToolExecutionContext context = new ToolExecutionContext(
                userId,
                scheduleId,
                request.getDate(),
                request.isApply(),
                request.getAttractionId(),
                scope
        );

        List<Map<String, Object>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "developer",
                "content", buildDeveloperPrompt(scope)
        ));

        messages.add(Map.of(
                "role", "user",
                "content", buildUserPrompt(request, scheduleId, scope)
        ));

        List<Map<String, Object>> tools = scheduleToolRegistry.getDefinitions(scope);

        ScheduleDetailResponse latestSchedule = null;
        String latestMessage;

        for (int round = 0; round < MAX_TOOL_CALL_ROUND; round++) {
            ToolChatResponse aiResponse = gmsOpenAIClient.chatWithTools(messages, tools);

            if (!aiResponse.hasToolCalls()) {
                latestMessage = aiResponse.getContent();

                String assistantMessage = latestMessage == null || latestMessage.isBlank()
                        ? "일정 명령 처리가 완료되었습니다."
                        : latestMessage;

                return AiScheduleCommandResponse.of(
                        assistantMessage,
                        latestSchedule
                );
            }

            messages.add(objectMapper.convertValue(
                    aiResponse.getAssistantMessage(),
                    Map.class
            ));

            for (ToolCall toolCall : aiResponse.getToolCalls()) {
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

    private String buildDeveloperPrompt(AiCommandScope scope) {
        return switch (scope) {
            case SCHEDULE_SCOPED -> buildScheduleScopedPrompt();
            case SCHEDULE_WORKFLOW -> buildScheduleWorkflowPrompt();
        };
    }

    private String buildCommonPrompt() {
        return """
                너는 여행 일정 편집 도우미다.
                사용자의 자연어 명령을 해석해서 제공된 tools 중 필요한 tool을 호출해야 한다.
                DB에 없는 nodeId, scheduleId, attractionId를 임의로 만들지 마라.
                Tool 호출 결과에 포함된 ID만 다음 Tool 호출에 사용하라.
                AI가 직접 DB를 수정하는 것이 아니라, 반드시 제공된 tool을 통해서만 작업을 수행해야 한다.
                최종 응답은 한국어로 짧게 작성한다.
                """;
    }

    private String buildScheduleScopedPrompt() {
        return buildCommonPrompt() + """
            
            현재 명령은 일정 상세 화면에서 들어왔다.
            scheduleId는 서버 context에 이미 있다.
            사용자의 '1번', '2번', '3번'은 DB nodeId가 아니라 화면에 보이는 visitOrder이다.
            
            [가능한 작업]
            - 현재 날짜의 노드 목록 조회
            - 현재 날짜 안에서 노드 순서 변경
            - 현재 일정의 미배치 노드 전체 날짜 자동 배치
            - 현재 일정 상세 조회
            
            [작업 규칙]
            - 날짜 기반 노드 순서 변경은 rearrange_nodes를 호출한다.
            - 장소 이름 기반 순서 변경은 필요하면 find_node로 nodeId를 찾은 뒤 rearrange_nodes를 호출한다.
            - 미배치 노드를 전체 날짜에 자동 배치하려면 auto_arrange_unscheduled_nodes를 호출한다.
            - 최종 일정 상태가 필요하면 get_schedule_detail을 호출한다.
            
            [금지]
            - 이 scope에서는 새 일정 생성이나 일정 검색을 하지 마라.
            - 이 scope에서는 관광지를 일정에 새로 추가하지 마라.
            - create_schedule, find_schedule, get_user_schedules, add_attraction_to_schedule을 호출하지 마라.
            """;
    }

    private String buildScheduleWorkflowPrompt() {
        return buildCommonPrompt() + """
            
            현재 명령은 관광지 목록 또는 관광지 상세 화면에서 들어올 수 있다.
            scheduleId가 없다.
            attractionId는 프론트가 현재 사용자가 보고 있거나 선택한 관광지를 확정해서 서버 context로 전달한다.
            
            [가능한 작업]
            - 사용자의 기존 일정 목록 조회
            - 일정 이름으로 기존 일정 찾기
            - 새 일정 생성
            - 선택된 관광지를 기존 일정 또는 새 일정에 미배치 노드로 추가
            - 관광지를 추가한 일정 상세 조회
            
            [작업 규칙]
            - 기존 일정에 추가하려면 find_schedule로 일정을 먼저 찾는다.
            - 사용자가 일정명을 명확히 말하지 않았다면 get_user_schedules로 후보를 확인할 수 있다.
            - 새 일정을 만들어 추가하려면 create_schedule을 먼저 호출한다.
            - 새 일정 생성에는 사용자가 명시한 title, startDate, endDate가 모두 필요하다.
            - 정보가 부족하면 tool을 호출하지 말고 사용자에게 필요한 정보를 물어봐라.
            - AI가 임의로 attractionId를 만들지 마라.
            - add_attraction_to_schedule에는 반드시 context attractionId만 사용하라.
            - 관광지를 추가한 뒤 최종 일정 상태가 필요하면 get_schedule_detail을 호출한다.
            
            [금지]
            - 이 scope에서는 노드 순서 변경을 하지 마라.
            - 이 scope에서는 미배치 노드 자동 배치를 하지 마라.
            - get_day_nodes, find_node, rearrange_nodes, auto_arrange_unscheduled_nodes를 호출하지 마라.
            
            [주의]
            - 일정명이 여러 개 매칭되면 바로 추가하지 말고 사용자에게 선택을 요청하라.
            - 사용자가 "이 관광지", "여기", "이 장소"라고 말하면 context의 attractionId를 의미한다.
            """;
    }

    private String buildUserPrompt(
            AiScheduleCommandRequest request,
            Long scheduleId,
            AiCommandScope scope
    ) {
        return """
                명령 scope: %s
                대상 일정 ID: %s
                대상 날짜: %s
                실제 반영 여부: %s
                선택된 관광지 ID: %s
                사용자 명령: %s
                """.formatted(
                scope,
                scheduleId,
                request.getDate(),
                request.isApply(),
                request.getAttractionId(),
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