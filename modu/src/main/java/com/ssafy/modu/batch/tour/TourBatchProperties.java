package com.ssafy.modu.batch.tour;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tour-batch")
public class TourBatchProperties {


    private boolean enabled = false;

    private String cron = "0 30 4 * * *";

    private String zone = "Asia/Seoul";


    private Integer maxPages;

    private int detailBatchSize = 200;
}

