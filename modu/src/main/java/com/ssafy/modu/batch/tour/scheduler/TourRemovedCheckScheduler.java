package com.ssafy.modu.batch.tour.scheduler;

import com.ssafy.modu.batch.tour.dto.TourRemovedCheckResult;
import com.ssafy.modu.batch.tour.service.TourBatchFacade;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourRemovedCheckScheduler {

    private final TourBatchFacade tourBatchFacade;

    @Scheduled(
            cron = "${tour-removed-check.cron:0 0 5 ? * SUN}",
            zone = "${tour-removed-check.zone:Asia/Seoul}"
    )
    public void runRemovedCheck() {
        log.info("무장애 관광지 삭제 여부 검증 시작");

        try {
            TourRemovedCheckResult result =
                    tourBatchFacade.runRemovedAccessibleCheck();

            log.info(
                    "무장애 관광지 삭제 여부 검증 완료. currentApiCount={}, checkedDbCount={}, removedDbCount={}",
                    result.currentApiCount(),
                    result.checkedDbCount(),
                    result.removedDbCount()
            );

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TOUR_API_TRAFFIC_EXCEEDED) {
                log.warn(
                        "하루 API 사용량 초과로 삭제 검증 스케줄러를 종료합니다. errorCode={}, message={}",
                        e.getErrorCode().name(),
                        e.getMessage()
                );
                return;
            }

            log.error(
                    "삭제 검증 중 비즈니스 예외 발생. errorCode={}, message={}",
                    e.getErrorCode().name(),
                    e.getMessage(),
                    e
            );

        } catch (Exception e) {
            log.error("삭제 검증 스케줄러 에러 발생", e);
        }
    }
}