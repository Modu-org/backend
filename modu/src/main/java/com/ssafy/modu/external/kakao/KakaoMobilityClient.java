package com.ssafy.modu.external.kakao;

import com.ssafy.modu.external.kakao.dto.KakaoDirectionResponse;
import com.ssafy.modu.external.kakao.dto.RouteSummary;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMobilityClient {

    private final WebClient webClient;
    private final KakaoMobilityProperties properties;

    public RouteSummary getDirections(
            BigDecimal originLongitude,
            BigDecimal originLatitude,
            BigDecimal destinationLongitude,
            BigDecimal destinationLatitude
    ) {
        String origin = originLongitude + "," + originLatitude;
        String destination = destinationLongitude + "," + destinationLatitude;

        KakaoDirectionResponse response;

        try {
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis-navi.kakaomobility.com")
                            .path(KakaoMobilityPath.DIRECTIONS)
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .queryParam("priority", "RECOMMEND")
                            .queryParam("summary", "true")
                            .build()
                    )
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .bodyToMono(KakaoDirectionResponse.class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("카카오 길찾기 API 호출 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new BusinessException(ErrorCode.KAKAO_DIRECTIONS_API_ERROR);
        }

        if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
            log.error("카카오 길찾기 API 응답이 비어 있습니다.");
            throw new BusinessException(ErrorCode.KAKAO_DIRECTIONS_API_ERROR);
        }

        KakaoDirectionResponse.Route route = response.getRoutes().get(0);

        if (route.getResult_code() == null || route.getResult_code() != 0 || route.getSummary() == null) {
            log.error("카카오 길찾기 실패. resultCode={}, resultMsg={}",
                    route.getResult_code(),
                    route.getResult_msg()
            );
            throw new BusinessException(ErrorCode.KAKAO_DIRECTIONS_API_ERROR);
        }

        return new RouteSummary(
                route.getSummary().getDistance(),
                route.getSummary().getDuration()
        );
    }
}