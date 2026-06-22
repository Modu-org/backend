package com.ssafy.modu.batch.tour.service;

import com.ssafy.modu.batch.tour.cursor.TourBatchCursorJobType;
import com.ssafy.modu.batch.tour.dto.TourImportResult;
import com.ssafy.modu.batch.tour.dto.TourInitialLoadResult;
import com.ssafy.modu.batch.tour.cursor.TourBatchCursor;
import com.ssafy.modu.batch.tour.cursor.TourBatchCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourInitialLoadService {

    private final TourBatchFacade tourBatchFacade;
    private final TourBatchCursorRepository cursorRepository;

    @Transactional
    public TourInitialLoadResult runOneJob(TourBatchCursorJobType jobType, int maxPages) {
        TourBatchCursor cursor = getOrCreateCursor(jobType);

        if (cursor.isCompleted()) {
            log.info("Tour initial load skipped. jobType={} already completed.", jobType);

            return new TourInitialLoadResult(
                    jobType,
                    cursor.getNextPage(),
                    0,
                    cursor.getLastRequestedPage() == null ? 0 : cursor.getLastRequestedPage(),
                    null,
                    cursor.getTotalCount() == null ? 0 : cursor.getTotalCount(),
                    true,
                    0,
                    0
            );
        }

        int startPage = cursor.getNextPage();

        log.info(
                "Tour initial load started. jobType={}, startPage={}, maxPages={}",
                jobType,
                startPage,
                maxPages
        );

        TourImportResult result = switch (jobType) {
            case ACCESSIBLE_LIST -> tourBatchFacade.runAccessibleListImport(null, startPage, maxPages);
            case GENERAL_LIST -> tourBatchFacade.runGeneralListImport(null, startPage, maxPages);
            default -> throw new IllegalArgumentException("초기 적재용 jobType이 아닙니다: " + jobType);
        };

        cursor.updateAfterRun(result);
        cursorRepository.save(cursor);

        log.info(
                "Tour initial load finished. jobType={}, startPage={}, lastRequestedPage={}, nextPage={}, totalCount={}, completed={}, imported={}",
                jobType,
                result.startPage(),
                result.lastRequestedPage(),
                result.nextPage(),
                result.totalCount(),
                result.completed(),
                result.importedAttractions()
        );

        return TourInitialLoadResult.from(jobType, result);
    }

    @Transactional
    public void reset(TourBatchCursorJobType jobType) {
        TourBatchCursor cursor = getOrCreateCursor(jobType);
        cursor.reset();
        cursorRepository.save(cursor);

        log.info("Tour initial load cursor reset. jobType={}", jobType);
    }

    private TourBatchCursor getOrCreateCursor(TourBatchCursorJobType jobType) {
        return cursorRepository.findByJobType(jobType)
                .orElseGet(() -> cursorRepository.save(TourBatchCursor.create(jobType)));
    }
}