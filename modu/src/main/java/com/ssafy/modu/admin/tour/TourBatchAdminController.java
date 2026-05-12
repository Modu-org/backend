package com.ssafy.modu.admin.tour;

import com.ssafy.modu.batch.tour.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tour-batch")
public class TourBatchAdminController {

    private final TourBatchFacade tourBatchFacade;
    private final TourInitialLoadService tourInitialLoadService;

    /**
     * 일반 관광 목록(areaBasedSyncList2) 적재.
     *
     * modifiedTime 생략 시 전체 목록 적재.
     * startPage 생략 시 1페이지부터 시작.
     *
     * 예:
     * POST /admin/tour-batch/list/general?startPage=1&maxPages=10
     * POST /admin/tour-batch/list/general?startPage=11&maxPages=10
     */
    @PostMapping("/list/general")
    public TourImportResult importGeneralList(
            @RequestParam(required = false) String modifiedTime,
            @RequestParam(defaultValue = "1") Integer startPage,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runGeneralListImport(modifiedTime, startPage, maxPages);
    }

    /**
     * 무장애 관광 목록(areaBasedSyncList2) 적재.
     *
     * modifiedTime 생략 시 전체 목록 적재.
     * startPage 생략 시 1페이지부터 시작.
     *
     * 이 목록에 등장하면 accessibleCandidate=true로 마킹된다.
     *
     * 예:
     * POST /admin/tour-batch/list/accessible?startPage=1&maxPages=10
     * POST /admin/tour-batch/list/accessible?startPage=11&maxPages=10
     */
    @PostMapping("/list/accessible")
    public TourImportResult importAccessibleList(
            @RequestParam(required = false) String modifiedTime,
            @RequestParam(defaultValue = "1") Integer startPage,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runAccessibleListImport(modifiedTime, startPage, maxPages);
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
     *
     * 예:
     * POST /admin/tour-batch/sync/modified?modifiedTime=20260508000000&maxPages=3
     */
    @PostMapping("/sync/modified")
    public TourBatchSyncResult syncModified(
            @RequestParam String modifiedTime,
            @RequestParam(required = false) Integer maxPages
    ) {
        return tourBatchFacade.runModifiedSync(modifiedTime, maxPages);
    }
    @PostMapping("/initial-load/{jobType}")
    public TourInitialLoadResult runInitialLoadJob(
            @PathVariable TourBatchCursorJobType jobType,
            @RequestParam(defaultValue = "10") int maxPages
    ) {
        return tourInitialLoadService.runOneJob(jobType, maxPages);
    }

    @PostMapping("/initial-load/{jobType}/reset")
    public void resetInitialLoadCursor(
            @PathVariable TourBatchCursorJobType jobType
    ) {
        tourInitialLoadService.reset(jobType);
    }
}