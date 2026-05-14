package com.ssafy.modu.batch.tour.scheduler;

import com.ssafy.modu.batch.tour.config.TourInitialLoadProperties;
import com.ssafy.modu.batch.tour.cursor.TourBatchCursorJobType;
import com.ssafy.modu.batch.tour.service.TourInitialLoadService;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourInitialLoadScheduler {

    private final TourInitialLoadService initialLoadService;
    private final TourInitialLoadProperties props;

    /**
     * 초기 전체 적재용 스케줄러.
     *
     * 기존 TourBatchScheduler는 매일 변경분 동기화 + 상세 점진 처리용이고,
     * 이 Scheduler는 전체 목록을 page cursor 기반으로 처음부터 끝까지 적재하는 용도다.
     */
    @Scheduled(
            cron = "${tour-initial-load.cron:0 0 3 * * *}",
            zone = "${tour-initial-load.zone:Asia/Seoul}"
    )
    public void runInitialLoad() {
        if (!props.isEnabled()) {
            return;
        }

        int maxPages = props.getMaxPagesPerRun();

        log.info(
                "초기 배치 스케줄러 실행 시작. generalEnabled={}, accessibleEnabled={}, maxPagesPerRun={}",
                props.isGeneralEnabled(),
                props.isAccessibleEnabled(),
                maxPages
        );

        try {
            // 무장애 관광지
            if (props.isAccessibleEnabled()) {
                initialLoadService.runOneJob(TourBatchCursorJobType.ACCESSIBLE_LIST, maxPages);
            }

            // 일반 관광지
            if (props.isGeneralEnabled()) {
                initialLoadService.runOneJob(TourBatchCursorJobType.GENERAL_LIST, maxPages);
            }

            log.info("관광지 일반 정보 적재 완료");

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TOUR_API_TRAFFIC_EXCEEDED) {
                log.warn(
                        "하루 API 사용량 초과로 초기 배치 스케줄러를 종료합니다. errorCode={}, message={}",
                        e.getErrorCode().name(),
                        e.getMessage()
                );
                return;
            }

            log.error(
                    "비즈니스 예외 발생으로 초기 배치 스케줄러를 종료합니다. errorCode={}, message={}",
                    e.getErrorCode().name(),
                    e.getMessage(),
                    e
            );

        } catch (Exception e) {
            log.error("스케줄러 에러 발생으로 인한 종료.", e);
        }
    }
}