package com.ssafy.modu.batch.tour;

public record TourImportResult(
        int requestedPages,
        int importedAttractions,
        int skippedAttractions,
        int importedAccessibilityRows
) {
}
