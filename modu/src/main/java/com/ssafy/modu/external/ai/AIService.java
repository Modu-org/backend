package com.ssafy.modu.external.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchParsedResult;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AIClient aiClient;
    private final ObjectMapper objectMapper;

    /**
     * 음성 입력 텍스트를 구조화된 검색 파라미터로 변환한다.
     *
     * type에 따라 다른 프롬프트 템플릿을 사용한다.
     * - type 1: 관광지 검색 파라미터
     * - type 2, 3: 향후 확장 예정
     *
     * AI API를 호출하고, 응답 JSON을 VoiceSearchParsedResult로 변환한다.
     */
    public VoiceSearchParsedResult parseVoiceInput(String text, int type) {
        /*
            type에 따라 프롬프트를 생성한다.
            현재는 type 1(관광지 검색)만 지원한다.
         */
        String prompt = buildPrompt(text, type);

        /*
            AI API 호출.
            프롬프트를 전송하고 응답 텍스트(JSON 문자열)를 받는다.
         */
        String responseText = aiClient.generateContent(prompt);

        log.info("AI 응답: {}", responseText);

        /*
            응답 JSON을 VoiceSearchParsedResult로 변환한다.
            파싱 실패 시 VOICE_SEARCH_PARSE_FAILED 예외를 발생시킨다.
         */
        return parseResponse(responseText);
    }

    /**
     * type에 따라 프롬프트를 생성한다.
     *
     * type 1: 관광지 검색 파라미터 변환 프롬프트
     * 그 외: INVALID_VOICE_SEARCH_TYPE 예외 발생
     */
    private String buildPrompt(String text, int type) {
        return switch (type) {
            case 1 -> String.format(AIPromptTemplate.ATTRACTION_SEARCH, text);
            default -> throw new BusinessException(ErrorCode.INVALID_VOICE_SEARCH_TYPE);
        };
    }

    /**
     * AI 응답 텍스트를 VoiceSearchParsedResult로 변환한다.
     *
     * AI가 JSON만 출력하도록 프롬프트에서 지시하지만,
     * 만약 앞뒤에 불필요한 공백이나 줄바꿈이 있을 수 있으므로 trim 처리한다.
     */
    private VoiceSearchParsedResult parseResponse(String responseText) {
        try {
            String trimmed = responseText.trim();
            return objectMapper.readValue(trimmed, VoiceSearchParsedResult.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패. response={}", responseText, e);
            throw new BusinessException(ErrorCode.VOICE_SEARCH_PARSE_FAILED);
        }
    }
}
