package com.ssafy.modu.batch.tour;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.entity.enums.TourDetailLoadStatus;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.external.tourapi.TourApiService;
import com.ssafy.modu.external.tourapi.util.TourApiJsonExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourDataImportService {

    private static final int DEFAULT_NUM_OF_ROWS = 100;

    /**
     * 한 번의 상세 배치에서 처리할 기본 관광지 수.
     */
    private static final int DEFAULT_BATCH_SIZE = 200;

    /**
     * 한 번의 상세 배치에서 처리할 수 있는 최대 관광지 수.
     * 외부 API 호출량과 서버 부하를 고려해 제한한다.
     */
    private static final int MAX_BATCH_SIZE = 500;

    /**
     * 목록 동기화 배치 1회 실행 시 최대 요청 페이지 수.
     * null로 전체 적재를 허용하면 API 호출량이 과도해질 수 있으므로 기본 제한을 둔다.
     */
    private static final int DEFAULT_MAX_PAGES_PER_RUN = 100;

    private final TourApiService tourApiService;
    private final TourApiJsonExtractor extractor;
    private final AttractionRepository attractionRepository;
    private final AccessibilityImportService accessibilityImportService;

    /**
     * 일반 관광 목록을 적재한다.
     *
     * modifiedTime이 있으면 변경분 동기화로 동작하고,
     * 없으면 목록 API를 첫 페이지부터 조회한다.
     */
    public TourImportResult importGeneralList(String modifiedTime, Integer maxPages) {
        return importSyncList(modifiedTime, maxPages, false);
    }

    /**
     * 무장애 관광 목록을 적재한다.
     *
     * 무장애 목록에 등장한 관광지는 accessibleCandidate=true로 마킹하여
     * 이후 무장애 상세 정보 적재 대상이 되도록 한다.
     */
    public TourImportResult importAccessibleList(String modifiedTime, Integer maxPages) {
        return importSyncList(modifiedTime, maxPages, true);
    }

    /**
     * 무장애 후보 관광지에 대해 detailWithTour2를 호출하고 접근성 정보를 적재한다.
     *
     * item이 없으면 NO_DATA로 마킹하여 다음 배치에서 반복 호출되지 않도록 한다.
     * 개별 관광지 처리 중 실패해도 전체 배치가 중단되지 않고 다음 관광지를 계속 처리한다.
     */
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
                AccessibilityImportOutcome outcome = accessibilityImportService.importFromWithTour(attraction);
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
                log.warn(
                        "무장애 상세 정보 적재 실패. contentId={}, attractionId={}",
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

    /**
     * 무장애 후보 관광지에 대해 detailCommon2를 호출하고 공통 상세 정보를 보강한다.
     *
     * item이 있으면 관광지의 홈페이지, 개요, 이미지, 전화번호 등을 갱신한다.
     * item이 없으면 NO_DATA로 마킹하여 다음 배치에서 반복 호출되지 않도록 한다.
     */
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
                log.warn(
                        "관광지 공통 상세 정보 적재 실패. contentId={}, attractionId={}",
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

    /**
     * 일반/무장애 목록 API를 호출하여 관광지 목록을 upsert한다.
     *
     * accessible=true인 경우 KorWithService2를 호출하고,
     * 저장된 관광지를 무장애 상세 적재 후보로 마킹한다.
     */
    private TourImportResult importSyncList(String modifiedTime, Integer maxPages, boolean accessible) {
        int safeMaxPages = normalizeMaxPages(maxPages);

        int pageNo = 1;
        int requestedPages = 0;
        int importedAttractions = 0;
        int skippedAttractions = 0;

        while (requestedPages < safeMaxPages) {
            JsonNode root = accessible
                    ? tourApiService.getAccessibleSync(modifiedTime, pageNo, DEFAULT_NUM_OF_ROWS, "1")
                    : tourApiService.getSync(modifiedTime, pageNo, DEFAULT_NUM_OF_ROWS, "1");

            validateResult(root);

            List<JsonNode> items = extractor.items(root);
            if (items.isEmpty()) {
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

            int totalCount = extractor.totalCount(root);
            if (pageNo * DEFAULT_NUM_OF_ROWS >= totalCount) {
                break;
            }

            pageNo++;
        }

        return new TourImportResult(
                requestedPages,
                importedAttractions,
                skippedAttractions,
                0
        );
    }

    /**
     * 목록 API item을 Attraction 엔티티로 변환하여 저장한다.
     *
     * contentId가 이미 존재하면 기존 엔티티를 갱신하고,
     * 존재하지 않으면 새 엔티티를 생성한다.
     */
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

    /**
     * detailCommon2 응답을 이용해 관광지 공통 상세 정보를 보강한다.
     *
     * item이 없으면 false를 반환한다.
     */
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

    /**
     * 목록 저장에 필요한 최소 필드가 있는지 확인한다.
     */
    private boolean hasRequiredAttractionFields(JsonNode item) {
        return extractor.text(item, "contentid") != null
                && extractor.text(item, "contenttypeid") != null
                && extractor.text(item, "title") != null;
    }

    /**
     * Tour API 응답 코드가 정상인지 확인한다.
     */
    private void validateResult(JsonNode root) {
        String code = extractor.resultCode(root);

        if (code != null && !("0000".equals(code) || "00".equals(code))) {
            throw new IllegalStateException(
                    "Tour API 오류: " + code + " / " + extractor.resultMsg(root)
            );
        }
    }

    /**
     * 상세 배치 크기를 안전한 범위로 보정한다.
     */
    private int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    /**
     * 목록 동기화 페이지 수를 안전한 범위로 보정한다.
     */
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