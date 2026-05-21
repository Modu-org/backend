package com.ssafy.modu.external.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "gemini")
@Component
public class AIProperties {

    private String apiKey;
    private String model;
    private String baseUrl;
}
