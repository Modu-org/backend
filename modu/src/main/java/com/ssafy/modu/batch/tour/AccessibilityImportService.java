package com.ssafy.modu.batch.tour;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.*;
import com.ssafy.modu.domain.accessibility.repository.AccessibilityInfoRepository;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.external.tourapi.TourApiService;
import com.ssafy.modu.external.tourapi.util.TourApiJsonExtractor;
import com.ssafy.modu.global.util.mapper.AccessibilityFieldMapper;
import com.ssafy.modu.global.util.mapper.AccessibilityFieldMapping;
import com.ssafy.modu.global.util.parser.AccessibilityStatusParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccessibilityImportService {


    private final TourApiService tourApiService;
    private final TourApiJsonExtractor extractor;
    private final AccessibilityInfoRepository accessibilityInfoRepository;
    private final AccessibilityStatusParser statusParser;
    private final AccessibilityFieldMapper fieldMapper;

    /**
     * 무장애 detailWithTour2를 호출해 접근성 정보를 row 단위로 upsert한다.
     *
     * <p>주의: item이 없는 경우도 정상 케이스이므로, 호출 결과(hasItem)를 반환해
     * 상위 배치가 NO_DATA로 마킹할 수 있게 한다.</p>
     */
    @Transactional
    public AccessibilityImportOutcome importFromWithTour(Attraction attraction) {
        JsonNode root = tourApiService.getAccessibility(attraction.getContentId());
        JsonNode item = extractor.firstItem(root);
        if (item == null) {
            return AccessibilityImportOutcome.empty();
        }

        int saved = 0;

        for (Map.Entry<String, JsonNode> field : item.properties()) {
            String sourceField = field.getKey();

            if ("contentid".equalsIgnoreCase(sourceField)) {
                continue;
            }

            AccessibilityFieldMapping mapping = fieldMapper.mapWithTourField(sourceField);
            if (mapping == null) {
                continue;
            }

            String rawValue = field.getValue().asText();
            saveOrUpdate(
                    attraction,
                    AccessibilitySource.KOR_WITH_DETAIL_WITH_TOUR,
                    sourceField,
                    rawValue,
                    mapping
            );
            saved++;
        }

        return new AccessibilityImportOutcome(true, saved);
    }

    @Transactional
    public int importFromKorDetailIntro(Attraction attraction) {
        JsonNode root = tourApiService.getIntro(attraction.getContentId(), attraction.getContentTypeId());
        JsonNode item = extractor.firstItem(root);
        if (item == null) {
            return 0;
        }

        int saved = 0;

        for (Map.Entry<String, JsonNode> field : item.properties()) {
            String sourceField = field.getKey();

            AccessibilityFieldMapping mapping = fieldMapper.mapKorIntroField(sourceField);
            if (mapping == null) {
                continue;
            }

            String rawValue = field.getValue().asText();
            saveOrUpdate(
                    attraction,
                    AccessibilitySource.KOR_DETAIL_INTRO,
                    sourceField,
                    rawValue,
                    mapping
            );
            saved++;
        }

        return saved;
    }

    @Transactional
    public int importFromKorDetailInfo(Attraction attraction) {
        JsonNode root = tourApiService.getInfo(attraction.getContentId(), attraction.getContentTypeId());
        int saved = 0;

        for (JsonNode item : extractor.items(root)) {
            String infoName = extractor.text(item, "infoname");
            String infoText = extractor.text(item, "infotext");

            AccessibilityFieldMapping mapping = fieldMapper.mapKorInfoName(infoName);
            if (mapping == null) {
                continue;
            }

            saveOrUpdate(attraction, AccessibilitySource.KOR_DETAIL_INFO, infoName, infoText, mapping);
            saved++;
        }
        return saved;
    }

    private void saveOrUpdate(
            Attraction attraction,
            AccessibilitySource source,
            String sourceField,
            String rawValue,
            AccessibilityFieldMapping mapping
    ) {
        AccessibilityStatus status = statusParser.parse(rawValue);

        AccessibilityInfo info = accessibilityInfoRepository
                .findByAttractionAndSourceAndSourceField(attraction, source, sourceField)
                .orElseGet(() -> AccessibilityInfo.create(
                        attraction,
                        source,
                        sourceField,
                        rawValue,
                        mapping.category(),
                        mapping.type(),
                        status
                ));

        info.update(rawValue, mapping.category(), mapping.type(), status);
        accessibilityInfoRepository.save(info);
    }
}
