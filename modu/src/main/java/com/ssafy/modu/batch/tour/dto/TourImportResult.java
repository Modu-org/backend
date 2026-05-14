package com.ssafy.modu.batch.tour.dto;

public record TourImportResult(
        int startPage,
        int requestedPages,
        int lastRequestedPage,
        Integer nextPage,
        int totalCount,
        boolean completed,
        int importedAttractions,
        int skippedAttractions,
        int importedAccessibilityRows
) {
}