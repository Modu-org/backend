package com.ssafy.modu.admin.tour;

import com.ssafy.modu.batch.tour.TourBatchFacade;
import com.ssafy.modu.batch.tour.TourBatchResult;
import com.ssafy.modu.batch.tour.TourBatchSyncResult;
import com.ssafy.modu.batch.tour.TourImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tour-batch")
public class TourBatchAdminController {

    private final TourBatchFacade tourBatchFacade;

    /**
     * 일반 관광 목록(areaBasedSyncList2) 적재.
     * modifiedTime 생략 시 전체 목록 적재(파라미터 미전송).
     */
    @PostMapping("/list/general")
    public TourImportResult importGeneralList(
            @RequestParam(required = false) String modifiedTime,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runGeneralListImport(modifiedTime, maxPages);
    }

    /**
     * 무장애 관광 목록(areaBasedSyncList2) 적재.
     * 이 목록에 등장하면 accessibleCandidate=true로 마킹된다.
     */
    @PostMapping("/list/accessible")
    public TourImportResult importAccessibleList(
            @RequestParam(required = false) String modifiedTime,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runAccessibleListImport(modifiedTime, maxPages);
    }

    /**
     * 무장애 후보에 대해 detailWithTour2를 batchSize만큼 처리.
     * item이 없으면 NO_DATA로 마킹되어 재호출되지 않는다.
     */
    @PostMapping("/detail/accessible")
    public TourBatchResult importAccessibleDetail(
            @RequestParam(defaultValue = "200") int batchSize
    ) {
        return tourBatchFacade.runAccessibleDetailImport(batchSize);
    }

    /**
     * 무장애 후보에 대해 detailCommon2를 batchSize만큼 처리.
     * item이 없으면 NO_DATA로 마킹되어 재호출되지 않는다.
     */
    @PostMapping("/detail/common-accessible")
    public TourBatchResult importCommonDetailForAccessible(
            @RequestParam(defaultValue = "200") int batchSize
    ) {
        return tourBatchFacade.runCommonDetailForAccessible(batchSize);
    }

    /**
     * modifiedTime 기준 변경분 동기화.
     * 일반 목록 + 무장애 목록을 각각 호출해 결과를 함께 반환한다.
     */
    @PostMapping("/sync/modified")
    public TourBatchSyncResult syncModified(
            @RequestParam String modifiedTime,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runModifiedSync(modifiedTime, maxPages);
    }
}
