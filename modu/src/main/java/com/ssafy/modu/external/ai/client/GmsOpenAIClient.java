package com.ssafy.modu.external.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.external.ai.AIClient;
import com.ssafy.modu.external.ai.properties.GmsOpenAIProperties;
import com.ssafy.modu.external.ai.dto.tool.ToolCall;
import com.ssafy.modu.external.ai.dto.tool.ToolChatResponse;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmsOpenAIClient implements AIClient {

    private final WebClient webClient;
    private final GmsOpenAIProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String generateContent(String prompt) {
        return generateContent(
                "You are a helpful assistant. Return only the requested result.",
                prompt
        );
    }

    @Override
    public String generateContent(String developerPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of(
                                "role", "developer",
                                "content", developerPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        String responseBody;
        try {
            responseBody = webClient.post()
                    .uri(properties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GMS OpenAI API 호출 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.error("GMS OpenAI API 응답이 비어 있습니다.");
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode contentNode = root
                    .path("choices").path(0)
                    .path("message")
                    .path("content");

            if (contentNode.isMissingNode() || contentNode.isNull()) {
                log.error("GMS OpenAI 응답에서 content를 찾을 수 없습니다. response={}", responseBody);
                throw new BusinessException(ErrorCode.AI_API_ERROR);
            }

            return contentNode.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GMS OpenAI 응답 파싱 실패. response={}", responseBody, e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }


    // AI에게서 tool call 실행 설명서를 받기 위해 던져주는 프롬프트 작성
    public ToolChatResponse chatWithTools(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools
    ) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", messages,
                "tools", tools,
                /*
                {
                  "tool_calls": [
                    {
                      "id": "call_...",
                      "function": {
                        "name": "rearrange_nodes",
                        "arguments": "{\"operation\":\"MOVE_TO_LAST\",\"sourceOrder\":2}"
                      }
                    }
                  ]
                }
                 */
                "tool_choice", "auto"
        );

        String responseBody;
        try {
            responseBody = webClient.post()
                    .uri(properties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GMS OpenAI Tool Calling API 호출 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        if (responseBody == null || responseBody.isBlank()) {
            log.error("GMS OpenAI Tool Calling API 응답이 비어 있습니다.");
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messageNode = root.path("choices").path(0).path("message");

            if (messageNode.isMissingNode() || messageNode.isNull()) {
                log.error("GMS OpenAI Tool Calling 응답에서 message를 찾을 수 없습니다. response={}", responseBody);
                throw new BusinessException(ErrorCode.AI_API_ERROR);
            }

            String content = messageNode.path("content").isMissingNode() || messageNode.path("content").isNull()
                    ? null
                    : messageNode.path("content").asText();

            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallNodes = messageNode.path("tool_calls");

            // tool call 리스트를 가져옴
            if (toolCallNodes.isArray()) {
                for (JsonNode toolCallNode : toolCallNodes) {
                    String id = toolCallNode.path("id").asText();
                    String name = toolCallNode.path("function").path("name").asText();
                    String argumentsText = toolCallNode.path("function").path("arguments").asText("{}");
                    JsonNode arguments = objectMapper.readTree(argumentsText);
                    toolCalls.add(new ToolCall(id, name, arguments));
                }
            }

            return new ToolChatResponse(messageNode, content, toolCalls);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GMS OpenAI Tool Calling 응답 파싱 실패. response={}", responseBody, e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }

    public byte[] generateSpeech(String text) {
        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/audio/speech";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini-tts",
                "input", text,
                "voice", "alloy"
        );

        try {
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GMS OpenAI Speech API 호출 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        } catch (Exception e) {
            log.error("GMS OpenAI Speech API 호출 중 예상치 못한 오류 발생", e);
            throw new BusinessException(ErrorCode.AI_API_ERROR);
        }
    }
}