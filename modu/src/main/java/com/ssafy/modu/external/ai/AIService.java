package com.ssafy.modu.external.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.external.ai.dto.request.RouteRecommendAiRequest;
import com.ssafy.modu.external.ai.dto.response.RouteRecommendAiResponse;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchParsedResult;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchDetailResponse;
import com.ssafy.modu.external.ai.client.GmsGeminiClient;
import com.ssafy.modu.external.ai.client.GmsOpenAIClient;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final GmsGeminiClient gmsGeminiClient;
    private final GmsOpenAIClient gmsOpenAIClient;
    private final ObjectMapper objectMapper;

    /**
     * 음성 입력 텍스트를 구조화된 검색 파라미터로 변환한다.
     *
     * 기존 흐름 유지:
     * 1. type에 따라 프롬프트 생성
     * 2. GMS Gemini API 호출
     * 3. 응답 JSON을 VoiceSearchParsedResult로 변환
     */
    public VoiceSearchParsedResult parseVoiceInput(String text, int type) {
        /*
            type에 따라 프롬프트를 생성한다.
            현재는 type 1(관광지 검색)만 지원한다.
         */
        String prompt = buildPrompt(text, type);

        /*
            GMS Gemini API 호출.
            프롬프트를 전송하고 응답 텍스트(JSON 문자열)를 받는다.
         */
        String responseText = gmsGeminiClient.generateContent(prompt);

        log.info("Gemini 음성 검색 응답: {}", responseText);

        /*
            응답 JSON을 VoiceSearchParsedResult로 변환한다.
            파싱 실패 시 VOICE_SEARCH_PARSE_FAILED 예외를 발생시킨다.
         */
        return parseResponse(responseText);
    }

    /**
     * 여행 경로 추천.
     *
     * 경로 추천은 GMS OpenAI API를 사용한다.
     */
    public RouteRecommendAiResponse recommendRoute(RouteRecommendAiRequest request) {
        try {
            String inputJson = objectMapper.writeValueAsString(request);

            String developerPrompt = """
                너는 여행 일정 경로 추천 엔진이다.
        
                반드시 사용자가 제공한 nodes와 edges만 사용한다.
                입력에 없는 nodeId, attractionId, edgeId를 절대 생성하지 않는다.
                node를 누락하거나 중복해서 사용하지 않는다.
                node의 visitDate를 변경하지 않는다.
                각 날짜 안에서 visitOrder만 조정한다.
        
                각 day에는 startNodeId, endNodeId, startFixed, endFixed가 포함될 수 있다.
        
                startFixed가 true이면 startNodeId는 반드시 해당 날짜의 첫 번째 노드여야 한다.
                endFixed가 true이면 endNodeId는 반드시 해당 날짜의 마지막 노드여야 한다.
        
                startFixed가 false이면 startNodeId는 서버가 추론한 추천 시작점이다.
                가능하면 첫 번째 노드로 사용하되, 전체 이동 시간이 더 좋아지는 경우 조정할 수 있다.
        
                endFixed가 false이면 endNodeId는 서버가 추론한 추천 종료점이다.
                가능하면 마지막 노드로 사용하되, 전체 이동 시간이 더 좋아지는 경우 조정할 수 있다.
        
                날짜별 노드는 해당 날짜 안에서만 재정렬한다.
                다른 날짜로 node를 이동시키지 않는다.
        
                출력은 반드시 JSON만 반환한다.
                코드 블록(```)은 절대 사용하지 않는다.
                """;

            String userPrompt = String.format(
                    AIPromptTemplate.ROUTE_RECOMMENDATION,
                    inputJson
            );

            String responseText = gmsOpenAIClient.generateContent(
                    developerPrompt,
                    userPrompt
            );


            return objectMapper.readValue(
                    responseText.trim(),
                    RouteRecommendAiResponse.class
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 경로 추천 응답 파싱 실패", e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
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

    /**
     * AI 응답 텍스트를 VoiceSearchDetailResponse로 변환한다.
     */
    public VoiceSearchDetailResponse parseVoiceDetailRead(String responseText) {
        try {
            String trimmed = responseText.trim();
            return objectMapper.readValue(trimmed, VoiceSearchDetailResponse.class);
        } catch (Exception e) {
            log.error("AI 상세 읽기 응답 파싱 실패. response={}", responseText, e);
            throw new BusinessException(ErrorCode.VOICE_SEARCH_PARSE_FAILED);
        }
    }
}