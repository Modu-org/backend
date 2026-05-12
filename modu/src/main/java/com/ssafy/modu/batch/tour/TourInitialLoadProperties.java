package com.ssafy.modu.batch.tour;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tour-initial-load")
public class TourInitialLoadProperties {

    /**
     * 초기 전체 목록 적재 스케줄러 사용 여부.
     */
    private boolean enabled = false;

    /**
     * 일반 관광 목록 초기 적재 여부.
     */
    private boolean generalEnabled = false;

    /**
     * 무장애 관광 목록 초기 적재 여부.
     */
    private boolean accessibleEnabled = true;

    /**
     * 한 번 실행할 때 처리할 페이지 수.
     * 1 page = 100 rows.
     */
    private int maxPagesPerRun = 10;

    /**
     * 스케줄러 시간대.
     */
    private String zone = "Asia/Seoul";
}