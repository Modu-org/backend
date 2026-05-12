package com.ssafy.modu.batch.tour;

import com.ssafy.modu.external.tourapi.TourApiTrafficExceededException;
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
//            TourBatchSyncResult sync = tourBatchFacade.runModifiedSync(modifiedTime, props.getMaxPages());
//            log.info("Tour list sync finished. general={}, accessible={}", sync.general(), sync.accessible());

            TourBatchResult accessibilityDetail = tourBatchFacade.runAccessibleDetailImport(props.getDetailBatchSize());
            log.info("accessibility detail 적재 완료. result={}", accessibilityDetail);

            TourBatchResult commonDetail = tourBatchFacade.runCommonDetailForAccessible(props.getDetailBatchSize());
            log.info("관광지 부가 정보 적재 완료. result={}", commonDetail);
        } catch (TourApiTrafficExceededException e) {
            log.warn("하루 API 사용량 초과.", e);
        }
        catch (Exception e) {
            log.error("스케줄러 에러 발생으로 인한 종료.", e);
        }
    }
}
