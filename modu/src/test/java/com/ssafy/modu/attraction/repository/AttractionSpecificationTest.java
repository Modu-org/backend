package com.ssafy.modu.attraction.repository;
import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilitySource;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import com.ssafy.modu.domain.attraction.dto.condition.AttractionSearchCondition;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.attraction.repository.specification.AttractionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AttractionSpecificationTest {

    @Autowired
    private AttractionRepository attractionRepository;

    @Test
    @DisplayName("PHYSICAL 필터는 PHYSICAL 검색 타입을 가진 관광지만 조회한다")
    void search_physicalFilter() {
        // given
        Attraction physicalAttraction = saveAttraction(
                "1001",
                "휠체어 가능한 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                physicalAttraction,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction visualOnlyAttraction = saveAttraction(
                "1002",
                "점자 안내 관광지",
                "대구광역시 중구",
                "27",
                "333",
                "444",
                "12"
        );
        addAccessibility(
                visualOnlyAttraction,
                "braileblock",
                "점자블록 있음",
                AccessibilityCategory.VISUAL,
                AccessibilityType.BRAILLE_BLOCK,
                AccessibilityStatus.AVAILABLE
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .categories(List.of(AccessibilityCategory.PHYSICAL))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactly("1001");
    }

    @Test
    @DisplayName("PHYSICAL과 VISUAL을 동시에 선택하면 두 조건을 모두 만족하는 관광지만 조회한다")
    void search_physicalAndVisualFilter() {
        // given
        Attraction both = saveAttraction(
                "2001",
                "휠체어와 시각 안내 모두 있는 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                both,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );
        addAccessibility(
                both,
                "audioguide",
                "오디오 가이드 있음",
                AccessibilityCategory.VISUAL,
                AccessibilityType.AUDIO_GUIDE,
                AccessibilityStatus.AVAILABLE
        );

        Attraction physicalOnly = saveAttraction(
                "2002",
                "휠체어만 가능한 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                physicalOnly,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction visualOnly = saveAttraction(
                "2003",
                "시각 안내만 있는 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                visualOnly,
                "audioguide",
                "오디오 가이드 있음",
                AccessibilityCategory.VISUAL,
                AccessibilityType.AUDIO_GUIDE,
                AccessibilityStatus.AVAILABLE
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .categories(List.of(
                        AccessibilityCategory.PHYSICAL,
                        AccessibilityCategory.VISUAL
                ))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactly("2001");
    }

    @Test
    @DisplayName("UNAVAILABLE과 UNKNOWN 상태의 접근성 정보는 필터 결과에서 제외된다")
    void search_excludeInvalidAccessibilityStatus() {
        // given
        Attraction available = saveAttraction(
                "3001",
                "사용 가능한 휠체어 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                available,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction unavailable = saveAttraction(
                "3002",
                "이용 불가 휠체어 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                unavailable,
                "wheelchair",
                "휠체어 이용 불가",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.UNAVAILABLE
        );

        Attraction unknown = saveAttraction(
                "3003",
                "정보 없음 휠체어 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                unknown,
                "wheelchair",
                "휠체어 정보 없음",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.UNKNOWN
        );

        Attraction partial = saveAttraction(
                "3004",
                "일부 가능 휠체어 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                partial,
                "wheelchair",
                "휠체어 일부 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.PARTIAL
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .categories(List.of(AccessibilityCategory.PHYSICAL))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactlyInAnyOrder("3001", "3004");
    }

    @Test
    @DisplayName("COMMON으로 저장된 PARKING도 PHYSICAL 검색 타입에 포함되어 조회된다")
    void search_commonParkingMatchesPhysicalFilter() {
        // given
        Attraction parking = saveAttraction(
                "4001",
                "주차 가능한 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12"
        );
        addAccessibility(
                parking,
                "parking",
                "주차 가능",
                AccessibilityCategory.COMMON,
                AccessibilityType.PARKING,
                AccessibilityStatus.AVAILABLE
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .categories(List.of(AccessibilityCategory.PHYSICAL))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactly("4001");
    }

    @Test
    @DisplayName("지역, 시군구, 키워드, 콘텐츠 타입 조건이 함께 적용된다")
    void search_regionSigunguKeywordContentType() {
        // given
        Attraction matched = saveAttraction(
                "5001",
                "대구수목원",
                "대구광역시 달서구 화암로",
                "27",
                "140",
                "222",
                "12"
        );
        addAccessibility(
                matched,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction differentRegion = saveAttraction(
                "5002",
                "대구수목원 비슷한 곳",
                "부산광역시 어딘가",
                "26",
                "140",
                "222",
                "12"
        );
        addAccessibility(
                differentRegion,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction differentContentType = saveAttraction(
                "5003",
                "대구수목원 체험관",
                "대구광역시 달서구 화암로",
                "27",
                "140",
                "222",
                "14"
        );
        addAccessibility(
                differentContentType,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .regionCode("27")
                .sigunguCode("140")
                .keyword("수목원")
                .contentTypeIds(List.of("12"))
                .categories(List.of(AccessibilityCategory.PHYSICAL))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactly("5001");
    }

    @Test
    @DisplayName("showFlag가 false인 관광지는 필터 결과에서 제외된다")
    void search_excludeHiddenAttraction() {
        // given
        Attraction visible = saveAttraction(
                "6001",
                "노출 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12",
                true
        );
        addAccessibility(
                visible,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        Attraction hidden = saveAttraction(
                "6002",
                "숨김 관광지",
                "대구광역시 달서구",
                "27",
                "111",
                "222",
                "12",
                false
        );
        addAccessibility(
                hidden,
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .categories(List.of(AccessibilityCategory.PHYSICAL))
                .build();

        // when
        List<Attraction> result = attractionRepository.findAll(
                AttractionSpecification.search(condition)
        );

        // then
        assertThat(result)
                .extracting(Attraction::getContentId)
                .containsExactly("6001");
    }

    private Attraction saveAttraction(
            String contentId,
            String name,
            String address,
            String regionCode,
            String sigunguCode,
            String overview,
            String contentTypeId
    ) {
        return saveAttraction(
                contentId,
                name,
                address,
                regionCode,
                sigunguCode,
                overview,
                contentTypeId,
                true
        );
    }

    private Attraction saveAttraction(
            String contentId,
            String name,
            String address,
            String regionCode,
            String sigunguCode,
            String overview,
            String contentTypeId,
            boolean showFlag
    ) {
        Attraction attraction = Attraction.create(contentId, contentTypeId);

        attraction.updateFromApi(
                name,
                address,
                "상세 주소",
                "12345",
                new BigDecimal("35.8012345"),
                new BigDecimal("128.5123456"),
                contentTypeId,
                "053-000-0000",
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                overview,
                regionCode,
                sigunguCode,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "https://example.com",
                showFlag,
                null
        );

        return attractionRepository.save(attraction);
    }

    private void addAccessibility(
            Attraction attraction,
            String sourceField,
            String rawValue,
            AccessibilityCategory category,
            AccessibilityType type,
            AccessibilityStatus status
    ) {
        AccessibilityInfo accessibilityInfo = AccessibilityInfo.create(
                attraction,
                AccessibilitySource.KOR_WITH_DETAIL_WITH_TOUR,
                sourceField,
                rawValue,
                category,
                type,
                status
        );

        attraction.getAccessibilityInfos().add(accessibilityInfo);
        attractionRepository.saveAndFlush(attraction);
    }
}
