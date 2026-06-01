package com.ssafy.modu.external.ai.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gms.openai")
public class GmsOpenAIProperties {

    private String apiKey;
    private String model;
    private String baseUrl;
}