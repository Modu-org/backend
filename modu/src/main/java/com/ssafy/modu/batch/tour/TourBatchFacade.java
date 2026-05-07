package com.ssafy.modu.batch.tour;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourBatchFacade {

    private final TourDataImportService tourDataImportService;

    /**
     * Controller 계층에서 "배치 유스케이스"를 깔끔하게 묶어주는 Facade.
     * (컨트롤러가 서비스 내부 구현을 직접 조립하지 않도록 분리)
     */
    public TourImportResult runGeneralListImport(String modifiedTime, Integer maxPages) {
        return tourDataImportService.importGeneralList(modifiedTime, maxPages);
    }

    public TourImportResult runAccessibleListImport(String modifiedTime, Integer maxPages) {
        return tourDataImportService.importAccessibleList(modifiedTime, maxPages);
    }

    public TourBatchResult runAccessibleDetailImport(int batchSize) {
        return tourDataImportService.importAccessibleDetail(batchSize);
    }

    public TourBatchResult runCommonDetailForAccessible(int batchSize) {
        return tourDataImportService.importCommonDetailForAccessibleCandidates(batchSize);
    }

    public TourBatchSyncResult runModifiedSync(String modifiedTime, Integer maxPages) {
        TourImportResult general = runGeneralListImport(modifiedTime, maxPages);
        TourImportResult accessible = runAccessibleListImport(modifiedTime, maxPages);
        return new TourBatchSyncResult(general, accessible);
    }
}
