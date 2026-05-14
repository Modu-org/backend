package com.ssafy.modu.batch.tour.dto;

public record TourBatchSyncResult(
        TourImportResult general,
        TourImportResult accessible
) {
}

