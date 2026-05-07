package com.ssafy.modu.external.tourapi;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.modu.global.TourApiProperties;
import com.ssafy.modu.global.util.UriBuilderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiClient {

    private final WebClient webClient;
    private final UriBuilderUtil uriBuilder;
    private final TourApiProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode callKor(String path, Map<String, String> params) {
        return get(properties.getKorServiceBaseUrl(), path, params);
    }

    public JsonNode callKorWith(String path, Map<String, String> params) {
        return get(properties.getKorWithServiceBaseUrl(), path, params);
    }

    private JsonNode get(String baseUrl, String path, Map<String, String> params) {
        URI uri = uriBuilder.build(baseUrl, path, params);

        String body = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Tour API 응답이 비어 있습니다.");
        }

        String trimmed = body.trim();

        if (!trimmed.startsWith("{")) {
            throw new IllegalStateException("Tour API가 JSON이 아닌 응답을 반환했습니다. path=" + path);
        }

        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception e) {
            throw new IllegalStateException("Tour API JSON 파싱 실패. path=" + path, e);
        }
    }
}