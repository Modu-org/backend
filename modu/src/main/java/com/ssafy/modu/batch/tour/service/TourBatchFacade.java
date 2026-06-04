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

    /**
     * 기존 호환용.
     * startPage를 지정하지 않으면 1페이지부터 적재한다.
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
     * 기존 호환용.
     * startPage를 지정하지 않으면 1페이지부터 적재한다.
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

    /**
     * modifiedTime 기준 변경분 동기화.
     * Scheduler에서 사용하는 운영용 동기화 흐름이다.
     *
     * 이 메서드는 변경분 조회용이므로 startPage 없이 1페이지부터 maxPages만큼 조회한다.
     */
    public TourBatchSyncResult runModifiedSync(String modifiedTime, Integer maxPages) {
        TourImportResult general = runGeneralListImport(modifiedTime, maxPages);
        TourImportResult accessible = runAccessibleListImport(modifiedTime, maxPages);

        return new TourBatchSyncResult(general, accessible);
    }

    public TourRemovedCheckResult runRemovedAccessibleCheck() {
        return tourDataImportService.checkRemovedAccessibleAttractions();
    }
}