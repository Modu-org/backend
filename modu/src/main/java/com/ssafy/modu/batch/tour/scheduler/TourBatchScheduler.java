package com.ssafy.modu.batch.tour.scheduler;

import com.ssafy.modu.batch.tour.config.TourBatchProperties;
import com.ssafy.modu.batch.tour.dto.TourBatchResult;
import com.ssafy.modu.batch.tour.dto.TourBatchSyncResult;
import com.ssafy.modu.batch.tour.service.TourBatchFacade;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourBatchScheduler {

    private static final DateTimeFormatter TOUR_API_MODIFIED_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourBatchFacade tourBatchFacade;
    private final TourBatchProperties props;

    @Scheduled(cron = "${tour-batch.cron:0 30 4 * * *}", zone = "${tour-batch.zone:Asia/Seoul}")
    public void runDaily() {
        if (!props.isEnabled()) {
            return;
        }

        ZoneId zone = ZoneId.of(props.getZone());
        LocalDateTime since = LocalDate.now(zone).minusDays(1).atStartOfDay();
        String modifiedTime = since.format(TOUR_API_MODIFIED_TIME);

        log.info("스케줄러 실행 시작. modifiedTime={}, maxPages={}, detailBatchSize={}",
                modifiedTime, props.getMaxPages(), props.getDetailBatchSize());

        try {
            // 변경 사항이나 새로 생긴 관광지 있는지 파악
            TourBatchSyncResult sync =
                    tourBatchFacade.runModifiedSync(modifiedTime, props.getMaxPages());

            log.info("Tour modified sync finished. general={}, accessible={}",
                    sync.general(),
                    sync.accessible());

            // NOT_STARTED인 무장애 정보 조회
            TourBatchResult accessibilityDetail =
                    tourBatchFacade.runAccessibleDetailImport(props.getDetailBatchSize());

            log.info("accessibility detail 적재 완료. result={}", accessibilityDetail);

            // NOT_STARTED인 관광지 상세 정보 조회
            TourBatchResult commonDetail =
                    tourBatchFacade.runCommonDetailForAccessible(props.getDetailBatchSize());

            log.info("관광지 부가 정보 적재 완료. result={}", commonDetail);

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TOUR_API_TRAFFIC_EXCEEDED) {
                log.warn("하루 API 사용량 초과로 스케줄러를 종료합니다. errorCode={}, message={}",
                        e.getErrorCode().name(),
                        e.getMessage());
                return;
            }

            log.error("비즈니스 예외 발생으로 스케줄러를 종료합니다. errorCode={}, message={}",
                    e.getErrorCode().name(),
                    e.getMessage(),
                    e);

        } catch (Exception e) {
            log.error("스케줄러 에러 발생으로 인한 종료.", e);
        }
    }
}