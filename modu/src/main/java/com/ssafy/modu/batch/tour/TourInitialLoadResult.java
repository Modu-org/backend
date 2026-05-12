package com.ssafy.modu.batch.tour;

public record TourInitialLoadResult(
        TourBatchCursorJobType jobType,
        int startPage,
        int requestedPages,
        int lastRequestedPage,
        Integer nextPage,
        int totalCount,
        boolean completed,
        int importedAttractions,
        int skippedAttractions
) {
    public static TourInitialLoadResult from(TourBatchCursorJobType jobType, TourImportResult result) {
        return new TourInitialLoadResult(
                jobType,
                result.startPage(),
                result.requestedPages(),
                result.lastRequestedPage(),
                result.nextPage(),
                result.totalCount(),
                result.completed(),
                result.importedAttractions(),
                result.skippedAttractions()
        );
    }
}