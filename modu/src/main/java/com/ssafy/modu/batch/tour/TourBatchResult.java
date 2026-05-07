package com.ssafy.modu.batch.tour;

public record TourBatchResult(
        int processedAttractions,
        int updatedAttractions,
        int failedAttractions,
        int importedAccessibilityRows
) {
}

