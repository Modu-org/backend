package com.ssafy.modu.batch.tour.dto;

public record TourBatchResult(
        int processedAttractions,
        int updatedAttractions,
        int failedAttractions,
        int importedAccessibilityRows
) {
}

