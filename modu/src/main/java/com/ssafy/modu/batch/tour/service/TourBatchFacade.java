package com.ssafy.modu.batch.tour.service;

import com.ssafy.modu.batch.tour.dto.TourBatchResult;
import com.ssafy.modu.batch.tour.dto.TourBatchSyncResult;
import com.ssafy.modu.batch.tour.dto.TourImportResult;
import com.ssafy.modu.batch.tour.dto.TourRemovedCheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourBatchFacade {

    private final TourDataImportService tourDataImportService;
    private final TourModifiedSyncService tourModifiedSyncService;

    /**
     * 변경분 동기화.
     *
     * Scheduler는 이 메서드만 호출한다.
     * 실제 변경분 cursor 관리는 TourModifiedSyncService가 담당한다.
     */
    public TourBatchSyncResult runModifiedSync(String modifiedTime, Integer maxPages) {
        return tourModifiedSyncService.runModifiedSync(modifiedTime, maxPages);
    }

    /**
     * 일반 관광 목록을 1페이지부터 maxPages만큼 적재한다.
     *
     * 초기 적재 또는 수동 호출용.
     * 운영 변경분 동기화에서는 cursor 기반 메서드를 사용한다.
     */
    public TourImportResult runGeneralListImport(String modifiedTime, Integer maxPages) {
        return tourDataImportService.importGeneralList(modifiedTime, maxPages);
    }

    /**
     * 일반 관광 목록을 startPage부터 maxPages만큼 적재한다.
     */
    public TourImportResult runGeneralListImport(String modifiedTime, Integer startPage, Integer maxPages) {
        return tourDataImportService.importGeneralList(modifiedTime, startPage, maxPages);
    }

    /**
     * 무장애 관광 목록을 1페이지부터 maxPages만큼 적재한다.
     *
     * 초기 적재 또는 수동 호출용.
     * 운영 변경분 동기화에서는 cursor 기반 메서드를 사용한다.
     */
    public TourImportResult runAccessibleListImport(String modifiedTime, Integer maxPages) {
        return tourDataImportService.importAccessibleList(modifiedTime, maxPages);
    }

    /**
     * 무장애 관광 목록을 startPage부터 maxPages만큼 적재한다.
     */
    public TourImportResult runAccessibleListImport(String modifiedTime, Integer startPage, Integer maxPages) {
        return tourDataImportService.importAccessibleList(modifiedTime, startPage, maxPages);
    }

    public TourBatchResult runAccessibleDetailImport(int batchSize) {
        return tourDataImportService.importAccessibleDetail(batchSize);
    }

    public TourBatchResult runCommonDetailForAccessible(int batchSize) {
        return tourDataImportService.importCommonDetailForAccessibleCandidates(batchSize);
    }

    public TourRemovedCheckResult runRemovedAccessibleCheck() {
        return tourDataImportService.checkRemovedAccessibleAttractions();
    }
}