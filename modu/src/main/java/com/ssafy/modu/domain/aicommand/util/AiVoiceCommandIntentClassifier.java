package com.ssafy.modu.domain.aicommand.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.domain.aicommand.dto.enums.VoiceCommandIntent;
import com.ssafy.modu.domain.aicommand.dto.request.VoiceCommandRequest;
import com.ssafy.modu.external.ai.client.GmsGeminiClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiVoiceCommandIntentClassifier {

    private final GmsGeminiClient gmsGeminiClient;
    private final ObjectMapper objectMapper;

    public VoiceIntentClassificationResult classify(VoiceCommandRequest request) {
        String prompt = buildPrompt(request);
        String response = gmsGeminiClient.generateContent(prompt);

        return parseResponse(response);
    }

    private String buildPrompt(VoiceCommandRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("text", request.getText());
        input.put("screen", request.getScreen());
        input.put("attractionId", request.getAttractionId());
        input.put("scheduleId", request.getScheduleId());
        input.put("date", request.getDate() != null ? request.getDate().toString() : null);
        input.put("visibleAttractions", request.getVisibleAttractions() != null
                ? request.getVisibleAttractions()
                : List.of());
        String inputJson = toJson(input);

        return """
                너는 음성 명령 의도 분류기다.
                실제 작업을 수행하지 말고, 사용자의 명령이 어떤 기능으로 가야 하는지만 분류한다.
                
                가능한 intent는 아래 5개 중 하나다.
                
                1. ATTRACTION_SEARCH
                - 관광지 검색, 추천, 목록 조회
                - 예: "부산 관광지 찾아줘", "휠체어 가능한 관광지 추천해줘"
                
                2. ATTRACTION_DETAIL
                - 특정 관광지의 상세 정보, 설명, 안내, 읽어주기
                - 예: "이 관광지 설명해줘", "해운대해수욕장 자세히 알려줘"
                
                3. SCHEDULE_WORKFLOW
                - 관광지를 일정에 추가
                - 기존 일정 찾기, 새 일정 생성 후 관광지 추가
                - 예: "이 관광지 부산여행에 추가해줘", "해운대해수욕장 일정에 넣어줘"
                
                4. SCHEDULE_SCOPED
                - 특정 일정 상세 화면 내부 명령
                - 노드 순서 변경, 자동 배치, 일정 내부 편집
                - 예: "3번이랑 4번 바꿔줘", "미배치 장소 자동 배치해줘"
                
                5. UNKNOWN
                - 위 intent 중 하나로 확정하기 어려움
                
                분류 규칙:
                - screen, attractionId, scheduleId, date, visibleAttractions는 현재 프론트 context다.
                - context가 부족해도 사용자의 의도 자체는 분류할 수 있다.
                - 단, 실제 실행 가능 여부는 서버 Router가 검증한다.
                - DB 수정, 화면 이동, Tool 호출은 절대 하지 않는다.
                - 반드시 JSON만 반환한다.
                - 마크다운 코드블록을 사용하지 마라.
                
                출력 형식:
                {
                  "intent": "ATTRACTION_SEARCH | ATTRACTION_DETAIL | SCHEDULE_WORKFLOW | SCHEDULE_SCOPED | UNKNOWN",
                  "confidence": "HIGH | MEDIUM | LOW",
                  "message": null 또는 사용자에게 되물을 문장
                }
                
                입력:
                %s
                """.formatted(inputJson);
    }

    private VoiceIntentClassificationResult parseResponse(String response) {
        try {
            String json = extractJson(response);
            VoiceIntentClassificationResult result =
                    objectMapper.readValue(json, VoiceIntentClassificationResult.class);

            if (result.getIntent() == null) {
                return VoiceIntentClassificationResult.unknown(
                        "어떤 작업을 원하시는지 조금 더 구체적으로 말씀해 주세요."
                );
            }

            return result;
        } catch (Exception e) {
            return VoiceIntentClassificationResult.unknown(
                    "어떤 작업을 원하시는지 조금 더 구체적으로 말씀해 주세요."
            );
        }
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        String trimmed = response.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed
                    .replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");

        if (start >= 0 && end >= start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Object valueOrNull(Object value) {
        return value == null ? null : value;
    }

    @Getter
    @NoArgsConstructor
    public static class VoiceIntentClassificationResult {

        private VoiceCommandIntent intent;
        private String confidence;
        private String message;

        public static VoiceIntentClassificationResult unknown(String message) {
            VoiceIntentClassificationResult result = new VoiceIntentClassificationResult();
            result.intent = VoiceCommandIntent.UNKNOWN;
            result.confidence = "LOW";
            result.message = message;
            return result;
        }
    }
}