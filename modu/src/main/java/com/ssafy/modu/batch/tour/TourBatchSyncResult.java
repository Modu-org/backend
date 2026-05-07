package com.ssafy.modu.batch.tour;

public record TourBatchSyncResult(
        TourImportResult general,
        TourImportResult accessible
) {
}

