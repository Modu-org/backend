package com.ssafy.modu.batch.tour.service;

import com.ssafy.modu.batch.tour.cursor.TourBatchCursor;
import com.ssafy.modu.batch.tour.cursor.TourBatchCursorJobType;
import com.ssafy.modu.batch.tour.cursor.TourBatchCursorRepository;
import com.ssafy.modu.batch.tour.dto.TourBatchSyncResult;
import com.ssafy.modu.batch.tour.dto.TourImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourModifiedSyncService {

    private final TourBatchFacade tourBatchFacade;
    private final TourBatchCursorRepository cursorRepository;

    @Transactional
    public TourBatchSyncResult runModifiedSync(String newModifiedTime, Integer maxPages) {
        TourImportResult general = runOneModifiedSyncJob(
                TourBatchCursorJobType.GENERAL_MODIFIED_SYNC,
                newModifiedTime,
                maxPages
        );

        TourImportResult accessible = runOneModifiedSyncJob(
                TourBatchCursorJobType.ACCESSIBLE_MODIFIED_SYNC,
                newModifiedTime,
                maxPages
        );

        return new TourBatchSyncResult(general, accessible);
    }

    private TourImportResult runOneModifiedSyncJob(
            TourBatchCursorJobType jobType,
            String newModifiedTime,
            Integer maxPages
    ) {
        TourBatchCursor cursor = getOrCreateCursor(jobType);

        if (cursor.isCompleted() || cursor.getRunningModifiedTime() == null) {
            cursor.startModifiedSync(newModifiedTime);
        }

        String modifiedTime = cursor.getRunningModifiedTime();
        int startPage = cursor.getNextPage();

        log.info(
                "Tour modified sync started. jobType={}, modifiedTime={}, startPage={}, maxPages={}",
                jobType,
                modifiedTime,
                startPage,
                maxPages
        );

        TourImportResult result = switch (jobType) {
            case GENERAL_MODIFIED_SYNC -> tourBatchFacade.runGeneralListImport(
                    modifiedTime,
                    startPage,
                    maxPages
            );

            case ACCESSIBLE_MODIFIED_SYNC -> tourBatchFacade.runAccessibleListImport(
                    modifiedTime,
                    startPage,
                    maxPages
            );

            default -> throw new IllegalArgumentException("지원하지 않는 변경분 동기화 jobType입니다: " + jobType);
        };

        cursor.updateAfterModifiedSync(result);
        cursorRepository.save(cursor);

        log.info(
                "Tour modified sync finished. jobType={}, modifiedTime={}, startPage={}, lastRequestedPage={}, nextPage={}, completed={}, imported={}",
                jobType,
                modifiedTime,
                result.startPage(),
                result.lastRequestedPage(),
                result.nextPage(),
                result.completed(),
                result.importedAttractions()
        );

        return result;
    }

    private TourBatchCursor getOrCreateCursor(TourBatchCursorJobType jobType) {
        return cursorRepository.findByJobType(jobType)
                .orElseGet(() -> cursorRepository.save(TourBatchCursor.create(jobType)));
    }
}