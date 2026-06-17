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
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
     * - 미배치된 장소들 자동 배치해줘
     */
    @Transactional
    public AiScheduleCommandResponse handleScheduleScopedCommand(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request
    ) {
        return handleCommandSafely(
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
     * - 6월 20일이 포함된 일정에 이 관광지 추가해줘
     */
    @Transactional
    public AiScheduleCommandResponse handleScheduleWorkflowCommand(
            Long userId,
            AiScheduleCommandRequest request
    ) {
        return handleCommandSafely(
                userId,
                null,
                request,
                AiCommandScope.SCHEDULE_WORKFLOW
        );
    }

    private AiScheduleCommandResponse handleCommandSafely(
            Long userId,
            Long scheduleId,
            AiScheduleCommandRequest request,
            AiCommandScope scope
    ) {
        try {
            return handleCommand(userId, scheduleId, request, scope);
        } catch (BusinessException e) {
            markRollbackOnlySafely();

            return AiScheduleCommandResponse.of(
                    toUserFriendlyMessage(e.getErrorCode(), scope),
                    null
            );
        } catch (Exception e) {
            markRollbackOnlySafely();

            return AiScheduleCommandResponse.of(
                    "요청을 처리하는 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.",
                    null
            );
        }
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

        for (int round = 0; round < MAX_TOOL_CALL_ROUND; round++) {
            ToolChatResponse aiResponse = gmsOpenAIClient.chatWithTools(messages, tools);
            validateAiResponse(aiResponse);

            if (!aiResponse.hasToolCalls()) {
                String assistantMessage = normalizeAssistantMessage(aiResponse.getContent());

                return AiScheduleCommandResponse.of(
                        assistantMessage,
                        latestSchedule
                );
            }

            validateToolCallResponse(aiResponse);

            messages.add(objectMapper.convertValue(
                    aiResponse.getAssistantMessage(),
                    Map.class
            ));

            for (ToolCall toolCall : aiResponse.getToolCalls()) {
                validateToolCall(toolCall);

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

    private void validateAiResponse(ToolChatResponse aiResponse) {
        if (aiResponse == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_SCHEDULE_RESPONSE);
        }
    }

    private void validateToolCallResponse(ToolChatResponse aiResponse) {
        if (aiResponse.getAssistantMessage() == null
                || aiResponse.getToolCalls() == null
                || aiResponse.getToolCalls().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_AI_SCHEDULE_RESPONSE);
        }
    }

    private void validateToolCall(ToolCall toolCall) {
        if (toolCall == null
                || toolCall.getId() == null
                || toolCall.getId().isBlank()
                || toolCall.getName() == null
                || toolCall.getName().isBlank()
                || toolCall.getArguments() == null) {
            throw new BusinessException(ErrorCode.INVALID_AI_SCHEDULE_RESPONSE);
        }
    }

    private String normalizeAssistantMessage(String message) {
        if (message == null || message.isBlank()) {
            return "일정 명령 처리가 완료되었습니다.";
        }

        return message;
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
                사용자가 해야 할 다음 행동이 있으면 한 문장으로 자연스럽게 안내한다.
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
            
            현재 명령은 홈, 관광지 목록, 관광지 상세 화면에서 들어올 수 있다.
            scheduleId가 없다.
            이 scope는 "이미 선택되었거나 VoiceCommandRouter에서 단일 확정된 관광지"를 일정에 추가하는 workflow이다.
            추가할 관광지는 서버 context의 attractionId로 전달된다.
            
            [요청 처리 방식]
            - 현재 요청에서 제공된 context와 사용자 명령만으로 처리 가능한 작업만 수행한다.
            - 필요한 정보가 부족하면 tool을 억지로 호출하지 말고, 사용자에게 필요한 정보를 질문하고 종료한다.
            - 프론트의 화면 이동, 후보 선택, 다음 음성 입력 유도는 이 service의 책임이 아니다.
            - 이 service는 현재 요청에 대한 응답 메시지와 필요한 경우 최신 schedule만 반환한다.
            - 이전 요청의 context를 기억한다고 가정하지 마라.
            - 이어지는 요청에서도 필요한 attractionId, scheduleId, date 등은 프론트 또는 Router가 다시 전달해야 한다.
            
            [가능한 작업]
            - 사용자의 기존 일정 목록 조회
            - 일정 이름, 시작일, 종료일, 특정 날짜 포함 여부로 기존 일정 찾기
            - 새 일정 생성
            - 선택된 단일 관광지를 기존 일정 또는 새 일정에 미배치 노드로 추가
            - 관광지를 추가한 일정 상세 조회
            
            [기존 일정 찾기 규칙]
            - 기존 일정에 추가하려면 find_schedule로 일정을 먼저 찾는다.
            - 사용자가 일정 이름을 말하면 find_schedule의 title을 사용한다.
            - 사용자가 일정 시작일을 말하면 find_schedule의 startDate를 사용한다.
            - 사용자가 일정 종료일을 말하면 find_schedule의 endDate를 사용한다.
            - 사용자가 "6월 20일 일정", "그날 일정", "해당 날짜 일정"처럼 특정 날짜를 말하면 find_schedule의 containsDate를 사용한다.
            - 사용자가 "6월 20일부터 6월 22일까지 일정"처럼 기간을 말하면 find_schedule의 startDate와 endDate를 함께 사용한다.
            - 일정 이름과 날짜 조건을 함께 말하면 find_schedule에 가능한 조건을 모두 넣어 더 정확히 찾는다.
            - 일정 이름, 시작일, 종료일, 포함 날짜를 모두 알 수 없으면 get_user_schedules로 후보를 확인하고 사용자에게 선택을 요청한다.
            - find_schedule 결과가 여러 개이면 바로 추가하지 말고 사용자에게 어느 일정에 추가할지 선택을 요청한다.
            - find_schedule 결과가 없으면 바로 새 일정을 만들지 말고, 기존 일정에 추가할지 새 일정을 만들지 사용자에게 다시 확인한다.
            
            [새 일정 생성 규칙]
            - 새 일정을 만들어 추가하려면 create_schedule을 먼저 호출한다.
            - 새 일정 생성에는 사용자가 명시한 title, startDate, endDate가 모두 필요하다.
            - 사용자가 일정 이름을 말하지 않았다면 title을 임의로 만들지 말고 사용자에게 일정 이름을 물어봐라.
            - 사용자가 시작일 또는 종료일을 말하지 않았다면 startDate/endDate를 임의로 만들지 말고 사용자에게 여행 날짜를 물어봐라.
            - 정보가 부족하면 tool을 호출하지 말고 사용자에게 필요한 정보를 물어봐라.
            
            [관광지 추가 규칙]
            - 추가할 관광지는 서버 context의 attractionId로만 판단한다.
            - AI가 임의로 attractionId를 만들지 마라.
            - add_attraction_to_schedule에는 반드시 context attractionId만 사용하라.
            - context attractionId가 없으면 add_attraction_to_schedule을 호출하지 마라.
            - 사용자가 관광지 이름을 말했더라도 context attractionId가 없으면 관광지 ID를 추측하지 마라.
            - 여러 관광지 후보 중 어떤 것을 추가할지 선택하는 작업은 VoiceCommandRouter 또는 프론트 flow에서 처리한다.
            - 이 service에서는 후보 관광지를 비교하거나 선택하지 않는다.
            - 기존 일정 또는 새 일정이 확정된 뒤에만 add_attraction_to_schedule을 호출한다.
            - 관광지를 추가한 뒤 최종 일정 상태가 필요하면 get_schedule_detail을 호출한다.
            
            [금지]
            - 이 scope에서는 노드 순서 변경을 하지 마라.
            - 이 scope에서는 미배치 노드 자동 배치를 하지 마라.
            - get_day_nodes, find_node, rearrange_nodes, auto_arrange_unscheduled_nodes를 호출하지 마라.
            - 사용자가 자동 배치를 요청하더라도 이 scope에서는 실행하지 말고, 일정 상세 화면에서 실행할 수 있다고 안내한다.
            
            [주의]
            - 일정명이 여러 개 매칭되면 바로 추가하지 말고 사용자에게 선택을 요청하라.
            - 날짜 조건으로 여러 일정이 매칭되어도 바로 추가하지 말고 사용자에게 선택을 요청하라.
            - 사용자가 "이 관광지", "여기", "이 장소"라고 말하면 context의 attractionId를 의미한다.
            - 사용자가 "이 일정", "거기 일정"처럼 지시어만 말하고 일정명이나 날짜 조건을 말하지 않으면 get_user_schedules로 후보를 확인하거나 사용자에게 구체적인 일정을 물어봐라.
            - 단, 이 service는 이전 턴의 관광지나 일정 정보를 기억하지 않는다. 필요한 정보가 현재 요청 context에 없으면 사용자에게 다시 요청하라.
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

    private String toUserFriendlyMessage(ErrorCode errorCode, AiCommandScope scope) {
        return switch (errorCode) {
            case INVALID_AI_TOOL_ARGUMENTS -> invalidToolArgumentsMessage(scope);
            case UNSUPPORTED_AI_TOOL -> "요청하신 작업은 지금 화면에서는 처리할 수 없어요.";
            case UNSUPPORTED_AI_TOOL_OPERATION -> "지원하지 않는 일정 변경 방식이에요. 순서 변경이나 위치 이동 방식으로 다시 말씀해 주세요.";
            case INVALID_AI_SCHEDULE_RESPONSE -> "일정 처리 결과를 확인하는 중 문제가 생겼어요. 다시 한 번 요청해 주세요.";
            case AI_TOOL_CALL_LIMIT_EXCEEDED -> "요청을 처리하는 데 시간이 조금 오래 걸리고 있어요. 명령을 조금 더 짧게 다시 말씀해 주세요.";
            case SCHEDULE_NOT_FOUND -> "해당 일정을 찾지 못했어요. 일정 이름이나 날짜를 다시 확인해 주세요.";
            case NODE_NOT_FOUND -> "요청하신 장소를 일정에서 찾지 못했어요. 장소 이름이나 순서를 다시 확인해 주세요.";
            case INVALID_NODE_VISIT_DATE -> "선택한 날짜에 맞지 않는 장소 이동 요청이에요. 일정 날짜를 다시 확인해 주세요.";
            case AI_API_ERROR -> "AI 응답을 처리하는 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.";
            default -> "요청을 처리하지 못했어요. 내용을 조금 더 구체적으로 다시 말씀해 주세요.";
        };
    }

    private String invalidToolArgumentsMessage(AiCommandScope scope) {
        if (scope == AiCommandScope.SCHEDULE_WORKFLOW) {
            return "일정에 추가하려면 관광지와 대상 일정 정보가 필요해요. 관광지를 선택하고, 일정 이름이나 여행 날짜를 함께 말씀해 주세요.";
        }

        return "일정 변경에 필요한 정보가 부족해요. 변경할 장소의 순서나 날짜를 다시 말씀해 주세요.";
    }

    private void markRollbackOnlySafely() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception ignored) {
        }
    }
}