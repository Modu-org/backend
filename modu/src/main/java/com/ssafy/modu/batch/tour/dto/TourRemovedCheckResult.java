package com.ssafy.modu.batch.tour.dto;


public record TourRemovedCheckResult(
        int currentApiCount,
        int checkedDbCount,
        int removedDbCount
) {
}
