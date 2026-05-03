package com.ssafy.modu.global;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "tour-api")
@Component
public class TourApiProperties {

    private String serviceKey;
    private String mobileOs;
    private String mobileApp;
    private String responseType;

    private String korServiceBaseUrl;
    private String korWithServiceBaseUrl;
}