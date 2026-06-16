package com.ssafy.modu.domain.attraction.service;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.repository.AccessibilityInfoRepository;
import com.ssafy.modu.domain.attraction.dto.condition.AttractionSearchCondition;
import com.ssafy.modu.domain.attraction.dto.request.AttractionSearchRequest;
import com.ssafy.modu.domain.attraction.dto.response.AttractionAccessibilityResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.attraction.repository.specification.AttractionSpecification;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final AttractionRepository attractionRepository;
    private final AccessibilityInfoRepository accessibilityInfoRepository;

    /**
     * 관광지 목록 조회 메서드
     *
     * 변경된 접근성 필터 정책:
     * - 요청에 true로 들어온 값만 필터로 사용한다.
     * - 요청에 없거나 null인 값은 false로 처리한다.
     * - 사용자 프로필의 physical / visual / hearing / infantFamily 값은 관광지 조회 필터에 자동 반영하지 않는다.
     *
     * 예:
     * GET /api/attractions?visual=true&hearing=true
     *
     * 결과:
     * physical = false
     * infantFamily = false
     * visual = true
     * hearing = true
     */
    public Page<AttractionListResponse> searchAttractions(
            Long userId,
            AttractionSearchRequest request
    ) {
        /*
            이제 관광지 검색에서는 userId를 이용해서 UserDetail을 조회하지 않는다.

            이유:
            - 요청에 없는 필드는 사용자 기본 설정으로 대체하지 않고 false로 처리하기로 했기 때문이다.
            - 따라서 로그인 사용자의 프로필 값이 검색 조건에 섞이지 않는다.
         */
        List<AccessibilityCategory> categories = resolveCategories(request);

        List<String> sigunguCodes = resolveSigunguCodes(
                request.getRegionCode(),
                request.getSigunguCode()
        );

        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .regionCode(request.getRegionCode())
                .sigunguCode(request.getSigunguCode())
                .keyword(request.getKeyword())
                .sigunguCodes(sigunguCodes)
                .contentTypeIds(request.getContentTypeIds())
                .categories(categories)
                .build();

        Pageable pageable = createPageable(request);

        Page<Attraction> attractions = attractionRepository.findAll(
                AttractionSpecification.search(condition),
                pageable
        );

        List<Long> attractionIds = attractions.getContent().stream()
                .map(Attraction::getId)
                .toList();

        /*
            검색 결과는 선택한 카테고리 조건을 기준으로 필터링한다.

            다만, 검색 결과에 포함된 관광지의 accessibilityInfos는
            선택한 카테고리만 내려주지 않고, 해당 관광지가 가진 모든 AVAILABLE 무장애 정보를 제공한다.

            예:
            visual=true
            -> VISUAL 조건을 만족하는 관광지만 검색 결과에 포함
            -> 응답 accessibilityInfos에는 VISUAL뿐 아니라 PHYSICAL, HEARING, INFANT 등
               해당 관광지가 가진 모든 AVAILABLE 정보 포함
         */
        Map<Long, List<AttractionAccessibilityResponse>> accessibilityMap =
                accessibilityInfoRepository.findByAttraction_IdIn(attractionIds).stream()
                        .filter(info -> info.getStatus() == AccessibilityStatus.AVAILABLE)
                        .collect(Collectors.groupingBy(
                                info -> info.getAttraction().getId(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        infos -> infos.stream()
                                                .sorted(
                                                        Comparator
                                                                .comparing((AccessibilityInfo info) -> info.getCategory().name())
                                                                .thenComparing(info -> info.getType().name())
                                                )
                                                .map(AttractionAccessibilityResponse::from)
                                                .toList()
                                )
                        ));

        return attractions.map(attraction ->
                AttractionListResponse.from(
                        attraction,
                        accessibilityMap.getOrDefault(attraction.getId(), List.of())
                )
        );
    }

    /**
     * 관광지 상세 조회 메서드
     *
     * 상세 조회에서는 관광지의 전체 접근성 정보를 보여주는 것이 자연스럽다.
     * 따라서 목록 조회처럼 선택 카테고리 필터를 적용하지 않는다.
     */
    public AttractionDetailResponse getAttractionDetail(Long attractionId) {
        Attraction attraction = attractionRepository.findWithAccessibilityInfosByIdAndShowFlagTrue(attractionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND));

        return AttractionDetailResponse.from(attraction);
    }

    /**
     * 요청값을 바탕으로 접근성 카테고리 목록을 만든다.
     *
     * 변경된 정책:
     * - 요청값이 true이면 해당 카테고리를 추가한다.
     * - 요청값이 false이면 추가하지 않는다.
     * - 요청값이 null이면 false로 처리한다.
     *
     * Boolean.TRUE.equals(...)를 사용하면 null도 안전하게 false 처리된다.
     */
    private List<AccessibilityCategory> resolveCategories(
            AttractionSearchRequest request
    ) {
        List<AccessibilityCategory> result = new ArrayList<>();

        if (Boolean.TRUE.equals(request.getPhysical())) {
            result.add(AccessibilityCategory.PHYSICAL);
        }

        if (Boolean.TRUE.equals(request.getInfantFamily())) {
            result.add(AccessibilityCategory.INFANT_FAMILY);
        }

        if (Boolean.TRUE.equals(request.getVisual())) {
            result.add(AccessibilityCategory.VISUAL);
        }

        if (Boolean.TRUE.equals(request.getHearing())) {
            result.add(AccessibilityCategory.HEARING);
        }

        return result;
    }

    private Pageable createPageable(AttractionSearchRequest request) {
        int page = request.getPage() == null || request.getPage() < 0
                ? DEFAULT_PAGE
                : request.getPage();

        int size = request.getSize() == null
                ? DEFAULT_SIZE
                : Math.min(Math.max(request.getSize(), 1), MAX_SIZE);

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "apiModifiedTime")
        );
    }

    private List<String> resolveSigunguCodes(String regionCode, String sigunguCode) {
        if (sigunguCode == null || sigunguCode.isBlank()) {
            return List.of();
        }

        // 전북특별자치도 전주시 전체 예외 처리
        if ("52".equals(regionCode) && "110".equals(sigunguCode)) {
            return List.of("111", "113");
        }

        return List.of(sigunguCode);
    }
}