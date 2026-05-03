package com.ssafy.modu.global.util;

import com.ssafy.modu.global.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UriBuilderUtil {

    private final TourApiProperties properties;

    public URI build(String baseUrl, String path, Map<String, String> params) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + path)
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", properties.getResponseType());

        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                builder.queryParam(k, v);
            }
        });

        return builder.build(false).encode().toUri();
    }
}