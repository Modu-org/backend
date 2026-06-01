package com.ssafy.modu.external.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.external.ai.properties.GmsGeminiProperties;
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
public class GmsGeminiClient {

    private final WebClient webClient;
    private final GmsGeminiProperties properties;
    private final ObjectMapper objectMapper;

    public String generateContent(String prompt) {
        String url = String.format(
                "%s/models/%s:generateContent",
                properties.getBaseUrl(),
                properties.getModel()
        );

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
                    .header("x-goog-api-key", properties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GMS Gemini API 호출 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.error("GMS Gemini API 응답이 비어 있습니다.");
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode textNode = root
                    .path("candidates").path(0)
                    .path("content")
                    .path("parts").path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                log.error("GMS Gemini API 응답에서 텍스트를 찾을 수 없습니다. response={}", responseBody);
                throw new BusinessException(ErrorCode.AI_API_ERROR);
            }

            return textNode.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GMS Gemini API 응답 파싱 실패. response={}", responseBody, e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }
}