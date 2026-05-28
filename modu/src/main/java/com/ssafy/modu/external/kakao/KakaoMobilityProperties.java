package com.ssafy.modu.external.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.mobility")
public class KakaoMobilityProperties {

    private String restApiKey;
    private String baseUrl;
}