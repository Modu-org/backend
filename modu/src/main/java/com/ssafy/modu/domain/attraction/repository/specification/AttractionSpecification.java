package com.ssafy.modu.domain.attraction.repository.specification;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import com.ssafy.modu.domain.attraction.dto.condition.AttractionSearchCondition;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttractionSpecification {

    /*
        접근성 필터에서 "검색 가능한 상태"로 인정할 상태값 목록.

        AVAILABLE:
        - 명확히 사용 가능

        PARTIAL:
        - 일부 가능, 제한적 가능 등
        - 무장애 여행 서비스에서는 완전 제외하기보다 결과에 포함하고 표시하는 쪽이 낫다.

        NEED_CHECK는 현재 제외한다.
        - "문의", "현장 확인 필요" 같은 데이터가 너무 넓게 검색 결과에 섞일 수 있기 때문이다.
        - 필요하면 나중에 포함 여부를 정책으로 바꿀 수 있다.
     */
    private static final List<AccessibilityStatus> DEFAULT_VALID_STATUSES = List.of(
            AccessibilityStatus.AVAILABLE,
            AccessibilityStatus.PARTIAL
    );

    /*
        검색 매칭에 사용할 "의미 있는 타입" 목록.

        중요:
        - AccessibilityCategory enum 자체는 API 명세 기반으로 유지한다.
        - 이 Map은 "검색 정책"만 따로 분리한 것이다.

        왜 필요한가?
        - 기존 category.getSearchTypes()에는 PARKING, PUBLIC_TRANSPORT, ROUTE, RESTROOM 같은
          범용 타입이 포함되어 있다.
        - 이 값을 그대로 검색에 쓰면 visual/hearing 검색에서도
          주차장, 화장실, 이동경로 같은 보조 정보만 있는 관광지가 결과에 포함될 수 있다.
        - 그래서 검색 매칭에는 각 사용자 유형에 실질적으로 의미 있는 핵심 타입만 사용한다.
     */
    private static final Map<AccessibilityCategory, List<AccessibilityType>> MEANINGFUL_SEARCH_TYPES = Map.of(
            AccessibilityCategory.PHYSICAL, List.of(
                    AccessibilityType.ACCESSIBLE_PARKING,
                    AccessibilityType.WHEELCHAIR,
                    AccessibilityType.EXIT,
                    AccessibilityType.ELEVATOR,
                    AccessibilityType.ACCESSIBLE_RESTROOM,
                    AccessibilityType.AUDITORIUM,
                    AccessibilityType.ROOM
            ),

            AccessibilityCategory.VISUAL, List.of(
                    AccessibilityType.BRAILLE_BLOCK,
                    AccessibilityType.HELP_DOG,
                    AccessibilityType.GUIDE_HUMAN,
                    AccessibilityType.AUDIO_GUIDE,
                    AccessibilityType.BIG_PRINT,
                    AccessibilityType.BRAILLE_PROMOTION,
                    AccessibilityType.GUIDE_SYSTEM
            ),

            AccessibilityCategory.HEARING, List.of(
                    AccessibilityType.SIGN_GUIDE,
                    AccessibilityType.VIDEO_GUIDE,
                    AccessibilityType.GUIDE_SYSTEM
            ),

            AccessibilityCategory.INFANT_FAMILY, List.of(
                    AccessibilityType.STROLLER,
                    AccessibilityType.LACTATION_ROOM,
                    AccessibilityType.BABY_SPARE_CHAIR,
                    AccessibilityType.KIDS_FACILITY
            )
    );

    private AttractionSpecification() {
    }

    /**
     * 관광지 검색용 Specification 생성 메서드
     *
     * 적용 조건:
     * 1. showFlag = true
     * 2. 지역 코드
     * 3. 시군구 코드
     * 4. 키워드
     * 5. 콘텐츠 타입
     * 6. 접근성 필터
     *
     * 접근성 필터 정책:
     * - category IN (...) 방식이 아니다.
     * - 선택된 카테고리마다 EXISTS 조건을 따로 만든다.
     *
     * 예:
     * visual=true & hearing=true
     *
     * EXISTS(VISUAL 핵심 접근성 정보)
     * AND
     * EXISTS(HEARING 핵심 접근성 정보)
     *
     * 즉, 둘 중 하나만 만족하는 관광지는 제외된다.
     */
    public static Specification<Attraction> search(AttractionSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("showFlag")));

            addRegionCondition(predicates, root, cb, condition);
            addSigunguCondition(predicates, root, condition);
            addKeywordCondition(predicates, root, cb, condition);
            addContentTypeCondition(predicates, root, condition);
            addAccessibilityConditions(predicates, root, query, cb, condition);

            /*
                EXISTS 서브쿼리 기반이라 중복 가능성은 낮지만,
                추후 join 조건이 추가될 수 있으므로 distinct를 걸어둔다.
             */
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 지역 코드 조건 추가
     */
    private static void addRegionCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        if (hasText(condition.getRegionCode())) {
            predicates.add(cb.equal(root.get("lDongRegnCd"), condition.getRegionCode()));
        }
    }

    /**
     * 시군구 코드 조건 추가
     *
     * 전주시 예외 처리처럼 여러 시군구 코드가 들어올 수 있으므로
     * sigunguCodes 기준으로 IN 조건을 건다.
     */
    private static void addSigunguCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            AttractionSearchCondition condition
    ) {
        if (condition.getSigunguCodes() == null || condition.getSigunguCodes().isEmpty()) {
            return;
        }

        predicates.add(root.get("lDongSignguCd").in(condition.getSigunguCodes()));
    }

    /**
     * 키워드 검색 조건 추가
     */
    private static void addKeywordCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        if (!hasText(condition.getKeyword())) {
            return;
        }

        String keyword = "%" + condition.getKeyword().trim() + "%";

        predicates.add(cb.or(
                cb.like(root.get("name"), keyword),
                cb.like(root.get("address"), keyword)
        ));
    }

    /**
     * 콘텐츠 타입 조건 추가
     */
    private static void addContentTypeCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            AttractionSearchCondition condition
    ) {
        if (condition.getContentTypeIds() == null || condition.getContentTypeIds().isEmpty()) {
            return;
        }

        predicates.add(root.get("contentTypeId").in(condition.getContentTypeIds()));
    }

    /**
     * 접근성 조건 추가
     *
     * 기존 문제:
     * - category.getSearchTypes()만 기준으로 type을 검사하면
     *   visual/hearing 검색에 PARKING, RESTROOM 같은 범용 정보가 섞일 수 있다.
     *
     * 수정 정책:
     * - 선택된 category마다 EXISTS 조건을 만든다.
     * - EXISTS 내부에서 category도 반드시 일치시킨다.
     * - type은 MEANINGFUL_SEARCH_TYPES에 정의한 핵심 타입만 인정한다.
     */
    private static void addAccessibilityConditions(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        if (condition.getCategories() == null || condition.getCategories().isEmpty()) {
            return;
        }

        for (AccessibilityCategory category : condition.getCategories()) {
            predicates.add(hasMeaningfulAccessibilityInCategory(root, query, cb, category));
        }
    }

    /**
     * 특정 카테고리에 대해 의미 있는 접근성 정보가 하나 이상 존재하는지 검사한다.
     *
     * 예:
     * category = VISUAL이면
     *
     * 현재 관광지에 대해
     * category = VISUAL
     * AND type IN (BRAILLE_BLOCK, HELP_DOG, AUDIO_GUIDE, ...)
     * AND status IN (AVAILABLE, PARTIAL)
     *
     * 을 만족하는 AccessibilityInfo가 하나 이상 있어야 true가 된다.
     */
    private static Predicate hasMeaningfulAccessibilityInCategory(
            Root<Attraction> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            AccessibilityCategory category
    ) {
        List<AccessibilityType> meaningfulTypes = getMeaningfulSearchTypes(category);

        /*
            COMMON, ETC처럼 검색 매칭용 핵심 타입이 없는 카테고리는
            검색 조건으로 사용하지 않는다.

            현재 resolveCategories()에서는 PHYSICAL, VISUAL, HEARING, INFANT_FAMILY만 들어오므로
            일반적으로 이 분기는 거의 타지 않는다.
         */
        if (meaningfulTypes.isEmpty()) {
            return cb.conjunction();
        }

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AccessibilityInfo> accessibilityInfo = subquery.from(AccessibilityInfo.class);

        subquery.select(accessibilityInfo.get("id"))
                .where(
                        /*
                            현재 조회 중인 관광지와 연결된 접근성 정보만 검사한다.
                         */
                        cb.equal(accessibilityInfo.get("attraction"), root),

                        /*
                            핵심 1:
                            type만 보지 않고 category도 반드시 일치시킨다.

                            이 조건이 없으면 PHYSICAL 카테고리의 타입이
                            VISUAL/HEARING 검색에 섞일 수 있다.
                         */
                        cb.equal(accessibilityInfo.get("category"), category),

                        /*
                            핵심 2:
                            API 명세 기반 searchTypes 전체가 아니라,
                            서비스 검색 정책상 의미 있는 타입만 인정한다.
                         */
                        accessibilityInfo.get("type").in(meaningfulTypes),

                        /*
                            사용 가능 또는 부분 가능 정보만 검색 매칭으로 인정한다.
                         */
                        accessibilityInfo.get("status").in(DEFAULT_VALID_STATUSES)
                );

        return cb.exists(subquery);
    }

    private static List<AccessibilityType> getMeaningfulSearchTypes(AccessibilityCategory category) {
        return MEANINGFUL_SEARCH_TYPES.getOrDefault(category, List.of());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}