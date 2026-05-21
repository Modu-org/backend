package com.ssafy.modu.external.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClient {

    private final WebClient webClient;
    private final AIProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Gemini API에 프롬프트를 전송하고 응답 텍스트를 반환한다.
     *
     * 요청 형식:
     * POST {baseUrl}/models/{model}:generateContent?key={apiKey}
     * Body: { "contents": [{ "parts": [{ "text": prompt }] }] }
     *
     * 응답에서 candidates[0].content.parts[0].text를 추출하여 반환한다.
     *
     * 향후 다른 모델(OpenAI 등)을 사용할 경우,
     * 이 클래스에 새로운 메서드를 추가하거나 요청 형식을 분기하면 된다.
     */
    public String generateContent(String prompt) {
        String url = String.format(
                "%s/models/%s:generateContent?key=%s",
                properties.getBaseUrl(),
                properties.getModel(),
                properties.getApiKey()
        );

        /*
            Gemini API 요청 본문 구성.
            contents 배열 안에 parts 배열로 텍스트 프롬프트를 전달한다.
         */
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String responseBody;
        try {
            responseBody = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI API 호출 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.error("AI API 응답이 비어 있습니다.");
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        /*
            응답 JSON에서 텍스트를 추출한다.
            구조: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
         */
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root
                    .path("candidates").path(0)
                    .path("content")
                    .path("parts").path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                log.error("AI API 응답에서 텍스트를 찾을 수 없습니다. response={}", responseBody);
                throw new BusinessException(ErrorCode.AI_API_ERROR);
            }

            return textNode.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API 응답 파싱 실패. response={}", responseBody, e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }
}
