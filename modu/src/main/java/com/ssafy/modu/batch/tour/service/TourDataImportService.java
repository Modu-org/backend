package com.ssafy.modu.batch.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.batch.tour.dto.AccessibilityImportOutcome;
import com.ssafy.modu.batch.tour.dto.TourBatchResult;
import com.ssafy.modu.batch.tour.dto.TourImportResult;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.entity.enums.TourDetailLoadStatus;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.external.tourapi.TourApiService;
import com.ssafy.modu.external.tourapi.util.TourApiJsonExtractor;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataImportService {

    private static final int DEFAULT_NUM_OF_ROWS = 100;
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int DEFAULT_MAX_PAGES_PER_RUN = 400;

    private final TourApiService tourApiService;
    private final TourApiJsonExtractor extractor;
    private final AttractionRepository attractionRepository;
    private final AccessibilityImportService accessibilityImportService;

    public TourImportResult importGeneralList(String modifiedTime, Integer maxPages) {
        return importGeneralList(modifiedTime, 1, maxPages);
    }

    public TourImportResult importGeneralList(String modifiedTime, Integer startPage, Integer maxPages) {
        return importSyncList(modifiedTime, startPage, maxPages, false);
    }

    public TourImportResult importAccessibleList(String modifiedTime, Integer maxPages) {
        return importAccessibleList(modifiedTime, 1, maxPages);
    }

    public TourImportResult importAccessibleList(String modifiedTime, Integer startPage, Integer maxPages) {
        return importSyncList(modifiedTime, startPage, maxPages, true);
    }

    public TourBatchResult importAccessibleDetail(int batchSize) {
        int safeBatchSize = normalizeBatchSize(batchSize);

        List<Attraction> targets = attractionRepository
                .findByAccessibleCandidateTrueAndAccessibilityStatusOrderByApiModifiedTimeDesc(
                        TourDetailLoadStatus.NOT_STARTED,
                        PageRequest.of(0, safeBatchSize)
                );

        int processedAttractions = 0;
        int updatedAttractions = 0;
        int failedAttractions = 0;
        int importedAccessibilityRows = 0;

        for (Attraction attraction : targets) {
            processedAttractions++;

            try {
                AccessibilityImportOutcome outcome =
                        accessibilityImportService.importFromWithTour(attraction);

                importedAccessibilityRows += outcome.savedRows();

                if (outcome.hasItem()) {
                    attraction.markAccessibilityLoaded();
                } else {
                    attraction.markAccessibilityNoData();
                }

                attractionRepository.save(attraction);
                updatedAttractions++;

            } catch (Exception e) {
                failedAttractions++;

                if (isTrafficExceeded(e)) {
                    log.warn(
                            "Tour API traffic exceeded during accessibility detail import. Stop this run. contentId={}, attractionId={}",
                            attraction.getContentId(),
                            attraction.getId(),
                            e
                    );

                    throw new BusinessException(ErrorCode.TOUR_API_TRAFFIC_EXCEEDED);
                }

                log.warn(
                        "Accessibility detail import failed. contentId={}, attractionId={}",
                        attraction.getContentId(),
                        attraction.getId(),
                        e
                );
            }
        }

        return new TourBatchResult(
                processedAttractions,
                updatedAttractions,
                failedAttractions,
                importedAccessibilityRows
        );
    }

    public TourBatchResult importCommonDetailForAccessibleCandidates(int batchSize) {
        int safeBatchSize = normalizeBatchSize(batchSize);

        List<Attraction> targets = attractionRepository
                .findByAccessibleCandidateTrueAndCommonDetailStatusOrderByApiModifiedTimeDesc(
                        TourDetailLoadStatus.NOT_STARTED,
                        PageRequest.of(0, safeBatchSize)
                );

        int processedAttractions = 0;
        int updatedAttractions = 0;
        int failedAttractions = 0;

        for (Attraction attraction : targets) {
            processedAttractions++;

            try {
                boolean updated = importCommonDetail(attraction);

                if (updated) {
                    attraction.markCommonDetailLoaded();
                } else {
                    attraction.markCommonDetailNoData();
                }

                attractionRepository.save(attraction);
                updatedAttractions++;

            } catch (Exception e) {
                failedAttractions++;

                if (isTrafficExceeded(e)) {
                    log.warn(
                            "Tour API traffic exceeded during common detail import. Stop this run. contentId={}, attractionId={}",
                            attraction.getContentId(),
                            attraction.getId(),
                            e
                    );

                    throw new BusinessException(ErrorCode.TOUR_API_TRAFFIC_EXCEEDED);
                }

                log.warn(
                        "Common detail import failed. contentId={}, attractionId={}",
                        attraction.getContentId(),
                        attraction.getId(),
                        e
                );
            }
        }

        return new TourBatchResult(
                processedAttractions,
                updatedAttractions,
                failedAttractions,
                0
        );
    }

    private TourImportResult importSyncList(
            String modifiedTime,
            Integer startPage,
            Integer maxPages,
            boolean accessible
    ) {
        int safeStartPage = normalizeStartPage(startPage);
        int safeMaxPages = normalizeMaxPages(maxPages);

        int pageNo = safeStartPage;
        int requestedPages = 0;
        int importedAttractions = 0;
        int skippedAttractions = 0;

        int totalCount = 0;
        int lastRequestedPage = safeStartPage - 1;
        boolean completed = false;

        while (requestedPages < safeMaxPages) {
            JsonNode root;

            try {
                root = accessible
                        ? tourApiService.getAccessibleSync(modifiedTime, pageNo, DEFAULT_NUM_OF_ROWS, "1")
                        : tourApiService.getSync(modifiedTime, pageNo, DEFAULT_NUM_OF_ROWS, "1");

            } catch (Exception e) {
                if (isTrafficExceeded(e)) {
                    log.warn(
                            "Tour API traffic exceeded during sync list. Stop this run. accessible={}, pageNo={}",
                            accessible,
                            pageNo,
                            e
                    );

                    throw new BusinessException(ErrorCode.TOUR_API_TRAFFIC_EXCEEDED);
                }

                throw e;
            }

            validateResult(root);

            totalCount = extractor.totalCount(root);
            lastRequestedPage = pageNo;

            List<JsonNode> items = extractor.items(root);

            if (items.isEmpty()) {
                completed = true;
                break;
            }

            for (JsonNode item : items) {
                if (!hasRequiredAttractionFields(item)) {
                    skippedAttractions++;
                    continue;
                }

                Attraction attraction = upsertAttraction(item);
                attraction.markSyncedNow();

                if (accessible) {
                    attraction.markAccessibleCandidate();
                }

                attractionRepository.save(attraction);
                importedAttractions++;
            }

            requestedPages++;

            if (pageNo * DEFAULT_NUM_OF_ROWS >= totalCount) {
                completed = true;
                break;
            }

            pageNo++;
        }

        Integer nextPage = completed ? null : lastRequestedPage + 1;

        return new TourImportResult(
                safeStartPage,
                requestedPages,
                lastRequestedPage,
                nextPage,
                totalCount,
                completed,
                importedAttractions,
                skippedAttractions,
                0
        );
    }

    public Attraction upsertAttraction(JsonNode item) {
        String contentId = extractor.text(item, "contentid");
        String contentTypeId = extractor.text(item, "contenttypeid");

        Attraction attraction = attractionRepository.findByContentId(contentId)
                .orElseGet(() -> Attraction.create(contentId, contentTypeId));

        attraction.updateFromApi(
                extractor.text(item, "title"),
                extractor.text(item, "addr1"),
                extractor.text(item, "addr2"),
                extractor.text(item, "zipcode"),
                extractor.decimal(item, "mapy"),
                extractor.decimal(item, "mapx"),
                contentTypeId,
                extractor.text(item, "tel"),
                extractor.text(item, "firstimage"),
                extractor.text(item, "firstimage2"),
                null,
                extractor.text(item, "lDongRegnCd"),
                extractor.text(item, "lDongSignguCd"),
                extractor.text(item, "lclsSystm1"),
                extractor.text(item, "lclsSystm2"),
                extractor.text(item, "lclsSystm3"),
                extractor.dateTime(item, "createdtime"),
                extractor.dateTime(item, "modifiedtime"),
                null,
                extractor.boolByOneZero(item, "showflag"),
                extractor.text(item, "cpyrhtDivCd")
        );

        return attractionRepository.save(attraction);
    }

    public boolean importCommonDetail(Attraction attraction) {
        JsonNode root = tourApiService.getCommon(attraction.getContentId());
        JsonNode item = extractor.firstItem(root);

        if (item == null) {
            return false;
        }

        attraction.updateCommonDetail(
                extractor.text(item, "homepage"),
                extractor.text(item, "overview"),
                extractor.text(item, "tel"),
                extractor.text(item, "firstimage"),
                extractor.text(item, "firstimage2")
        );

        return true;
    }

    private boolean hasRequiredAttractionFields(JsonNode item) {
        return extractor.text(item, "contentid") != null
                && extractor.text(item, "contenttypeid") != null
                && extractor.text(item, "title") != null;
    }

    private void validateResult(JsonNode root) {
        String code = extractor.resultCode(root);

        if (code == null || "0000".equals(code) || "00".equals(code)) {
            return;
        }

        String msg = extractor.resultMsg(root);

        if (looksLikeTrafficExceeded(msg)) {
            throw new BusinessException(ErrorCode.TOUR_API_TRAFFIC_EXCEEDED);
        }

        throw new IllegalStateException("Tour API error: " + code + " / " + msg);
    }

    private boolean isTrafficExceeded(Throwable t) {
        Throwable cur = t;

        while (cur != null) {
            if (cur instanceof BusinessException businessException
                    && businessException.getErrorCode() == ErrorCode.TOUR_API_TRAFFIC_EXCEEDED) {
                return true;
            }

            cur = cur.getCause();
        }

        return false;
    }

    private boolean looksLikeTrafficExceeded(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }

        String m = msg.toLowerCase();

        return m.contains("traffic")
                || m.contains("quota")
                || m.contains("limit")
                || m.contains("exceed")
                || msg.contains("초과")
                || msg.contains("트래픽")
                || msg.contains("제한");
    }

    private int normalizeStartPage(Integer startPage) {
        if (startPage == null || startPage <= 0) {
            return 1;
        }

        return startPage;
    }

    private int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    private int normalizeMaxPages(Integer maxPages) {
        if (maxPages == null) {
            return DEFAULT_MAX_PAGES_PER_RUN;
        }

        if (maxPages <= 0) {
            return 1;
        }

        return Math.min(maxPages, DEFAULT_MAX_PAGES_PER_RUN);
    }
}