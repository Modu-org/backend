package com.ssafy.modu.domain.aicommand.service;

import com.ssafy.modu.domain.aicommand.dto.enums.VoiceCommandIntent;
import com.ssafy.modu.domain.aicommand.dto.enums.VoiceCommandResultType;
import com.ssafy.modu.domain.aicommand.dto.request.AiScheduleCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.request.VoiceCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.response.AiScheduleCommandResponse;
import com.ssafy.modu.domain.aicommand.dto.response.VoiceCommandResponse;
import com.ssafy.modu.domain.aicommand.util.AiVoiceCommandIntentClassifier;
import com.ssafy.modu.domain.aicommand.util.AiVoiceCommandIntentClassifier.VoiceIntentClassificationResult;
import com.ssafy.modu.domain.aicommand.util.VisibleAttractionResolver;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import com.ssafy.modu.domain.voicesearch.service.VoiceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceCommandRouter {

    private static final int VOICE_SEARCH_TYPE_SEARCH = 1;
    private static final int VOICE_SEARCH_TYPE_DETAIL = 2;

    private final AiVoiceCommandIntentClassifier intentClassifier;
    private final VoiceSearchService voiceSearchService;
    private final AiScheduleCommandService aiScheduleCommandService;
    private final VisibleAttractionResolver visibleAttractionResolver;
    private final ScheduleService scheduleService;

    public VoiceCommandResponse handle(Long userId, VoiceCommandRequest request) {
        VoiceIntentClassificationResult classification = intentClassifier.classify(request);
        VoiceCommandIntent intent = classification.getIntent();

        if (requiresLogin(intent) && userId == null) {
            return needMoreInfo("일정 기능을 사용하려면 로그인이 필요해요.");
        }

        return switch (intent) {
            case ATTRACTION_SEARCH -> handleAttractionSearch(userId, request);
            case ATTRACTION_DETAIL -> handleAttractionDetail(userId, request);
            case SCHEDULE_WORKFLOW -> handleScheduleWorkflow(userId, request);
            case SCHEDULE_SCOPED -> handleScheduleScoped(userId, request);
            case UNKNOWN -> needMoreInfo(
                    classification.getMessage() != null && !classification.getMessage().isBlank()
                            ? classification.getMessage()
                            : "어떤 작업을 원하시는지 조금 더 구체적으로 말씀해 주세요."
            );
        };
    }

    private boolean requiresLogin(VoiceCommandIntent intent) {
        return intent == VoiceCommandIntent.SCHEDULE_WORKFLOW
                || intent == VoiceCommandIntent.SCHEDULE_SCOPED;
    }

    private VoiceCommandResponse handleAttractionSearch(
            Long userId,
            VoiceCommandRequest request
    ) {
        Object data = voiceSearchService.search(
                request.getText(),
                VOICE_SEARCH_TYPE_SEARCH,
                userId,
                request.getAttractionId()
        );

        return VoiceCommandResponse.of(
                VoiceCommandResultType.ATTRACTION_SEARCH_RESULT,
                "조건에 맞는 관광지를 찾았습니다.",
                data
        );
    }

    private VoiceCommandResponse handleAttractionDetail(
            Long userId,
            VoiceCommandRequest request
    ) {
        Long attractionId = request.getAttractionId();

        if (attractionId == null) {
            attractionId = visibleAttractionResolver.resolveAttractionIdOrNull(request);
        }

        if (attractionId == null) {
            if (visibleAttractionResolver.hasMultipleMatches(request)) {
                return needMoreInfo("비슷한 이름의 관광지가 여러 개 있어요. 정확한 관광지 이름을 말씀해 주세요.");
            }

            return needMoreInfo("상세 정보를 볼 관광지를 정확히 알 수 없어요. 관광지를 선택하거나 정확한 이름을 말씀해 주세요.");
        }

        Object data = voiceSearchService.search(
                request.getText(),
                VOICE_SEARCH_TYPE_DETAIL,
                userId,
                attractionId
        );

        return VoiceCommandResponse.of(
                VoiceCommandResultType.ATTRACTION_DETAIL,
                "관광지 상세 정보를 찾았습니다.",
                data
        );
    }

    private VoiceCommandResponse handleScheduleWorkflow(
            Long userId,
            VoiceCommandRequest request
    ) {
        Long attractionId = request.getAttractionId();

        if (attractionId == null) {
            attractionId = visibleAttractionResolver.resolveAttractionIdOrNull(request);
        }

        if (attractionId == null) {
            if (visibleAttractionResolver.hasMultipleMatches(request)) {
                return needMoreInfo("비슷한 이름의 관광지가 여러 개 있어요. 정확한 관광지 이름을 말씀해 주세요.");
            }

            return needMoreInfo("추가할 관광지를 정확히 알 수 없어요. 관광지 이름을 정확히 말씀하거나 관광지를 선택한 뒤 다시 요청해 주세요.");
        }

        AiScheduleCommandRequest aiRequest = AiScheduleCommandRequest.of(
                request.getDate(),
                request.getText(),
                attractionId,
                request.isApply()
        );

        AiScheduleCommandResponse response =
                aiScheduleCommandService.handleScheduleWorkflowCommand(
                        userId,
                        aiRequest
                );

        return toScheduleWorkflowResponse(userId, response);
    }

    private VoiceCommandResponse handleScheduleScoped(
            Long userId,
            VoiceCommandRequest request
    ) {
        if (request.getScheduleId() == null) {
            return needMoreInfo("어떤 일정에서 변경할지 알 수 없어요.");
        }

        AiScheduleCommandResponse response =
                aiScheduleCommandService.handleScheduleScopedCommand(
                        userId,
                        request.getScheduleId(),
                        toAiScheduleCommandRequest(request)
                );

        return toScheduleScopedResponse(response);
    }

    private AiScheduleCommandRequest toAiScheduleCommandRequest(VoiceCommandRequest request) {
        return AiScheduleCommandRequest.of(
                request.getDate(),
                request.getText(),
                request.getAttractionId(),
                request.isApply()
        );
    }

    private VoiceCommandResponse toScheduleWorkflowResponse(
            Long userId,
            AiScheduleCommandResponse response
    ) {
        if (response.getSchedule() != null) {
            ScheduleSummaryResponse summary =
                    scheduleService.getScheduleSummary(
                            userId,
                            response.getSchedule().getScheduleId()
                    );

            return VoiceCommandResponse.of(
                    VoiceCommandResultType.SCHEDULE_UPDATED,
                    response.getAssistantMessage(),
                    summary
            );
        }

        String message = response.getAssistantMessage();

        if (isScheduleCandidateMessage(message)) {
            List<ScheduleSummaryResponse> schedules = scheduleService.getSchedules(userId);

            return VoiceCommandResponse.of(
                    VoiceCommandResultType.SCHEDULE_CANDIDATES,
                    message,
                    schedules
            );
        }

        if (isNeedMoreInfoMessage(message)) {
            return needMoreInfo(message);
        }

        return VoiceCommandResponse.of(
                VoiceCommandResultType.SIMPLE_MESSAGE,
                message,
                null
        );
    }

    private VoiceCommandResponse toScheduleScopedResponse(AiScheduleCommandResponse response) {
        if (response.getSchedule() != null) {
            return VoiceCommandResponse.of(
                    VoiceCommandResultType.SCHEDULE_UPDATED,
                    response.getAssistantMessage(),
                    response.getSchedule()
            );
        }

        String message = response.getAssistantMessage();

        if (isNeedMoreInfoMessage(message)) {
            return needMoreInfo(message);
        }

        return VoiceCommandResponse.of(
                VoiceCommandResultType.SIMPLE_MESSAGE,
                message,
                null
        );
    }

    private boolean isScheduleCandidateMessage(String message) {
        if (message == null) {
            return false;
        }

        return containsAny(
                message,
                "어떤 일정",
                "어느 일정",
                "일정을 선택",
                "일정 중",
                "일정 이름",
                "여행 날짜"
        );
    }

    private boolean isNeedMoreInfoMessage(String message) {
        if (message == null) {
            return false;
        }

        return containsAny(
                message,
                "필요",
                "알려주세요",
                "말씀해 주세요",
                "선택해 주세요",
                "확인해 주세요"
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private VoiceCommandResponse needMoreInfo(String message) {
        return VoiceCommandResponse.of(
                VoiceCommandResultType.NEED_MORE_INFO,
                message,
                null
        );
    }
}