package com.ssafy.modu.domain.attraction.repository.specification;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
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

public class AttractionSpecification {

    /*
        접근성 필터에서 "사용 가능"으로 인정할 상태값 목록.
        정보가 AVAILABLE 또는 PARTIAL이면 해당 관광지는 접근성 조건을 만족한다고 판단한다.
     */
    private static final List<AccessibilityStatus> DEFAULT_VALID_STATUSES = List.of(
            AccessibilityStatus.AVAILABLE,
            AccessibilityStatus.PARTIAL
    );
    private AttractionSpecification() {
    }

    /**
     * 관광지 검색용 Specification 생성 메서드
     *
     * 이 메서드에서 최종적으로 WHERE 조건을 조립한다.
     *
     * 적용되는 조건:
     * 1. showFlag = true
     * 2. 지역 코드 조건
     * 3. 시군구 코드 조건
     * 4. 키워드 조건
     * 5. 콘텐츠 타입 조건
     * 6. 접근성 카테고리 조건
     */
    public static Specification<Attraction> search(AttractionSearchCondition condition) {
        return (root, query, cb) -> {
            /*
                Predicate는 SQL의 WHERE 조건 하나를 의미한다고 보면 된다.

                예:
                show_flag = true
                name like '%수목원%'
                content_type_id in (...)
             */
            List<Predicate> predicates = new ArrayList<>();

            /*
                showFlag가 true인 관광지만 조회한다.
             */
            predicates.add(cb.isTrue(root.get("showFlag")));

            /*
                요청 조건에 따라 필요한 조건만 추가한다.
                condition 안에 값이 없으면 해당 조건은 추가하지 않는다.
             */
            addRegionCondition(predicates, root, cb, condition);
            addSigunguCondition(predicates, root, cb, condition);
            addKeywordCondition(predicates, root, cb, condition);
            addContentTypeCondition(predicates, root, condition);
            addAccessibilityConditions(predicates, root, query, cb, condition);

            // 지금까지 추가된 모든 조건을 AND로 묶는다.
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 지역 코드 조건 추가
     */


    /**
     * 시군구 코드 조건 추가
     */
    private static void addSigunguCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaBuilder cb,
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
    private static void addRegionCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        /*
            regionCode가 null이 아니고 빈 문자열도 아니면
            지역 코드 조건을 추가한다.
         */
        if (hasText(condition.getRegionCode())) {
            predicates.add(cb.equal(root.get("lDongRegnCd"), condition.getRegionCode()));
        }
    }
    private static void addKeywordCondition(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        /*
            keyword가 없으면 검색 조건을 추가하지 않고 바로 종료한다.
         */
        if (!hasText(condition.getKeyword())) {
            return;
        }

        /*
            LIKE 검색을 위해 앞뒤에 %를 붙인다.

            예:
            keyword = "수목원"
            -> "%수목원%"

            이렇게 하면 "대구수목원", "수목원길"처럼
            키워드가 포함된 데이터를 찾을 수 있다.
         */
        String keyword = "%" + condition.getKeyword().trim() + "%";

        /*
            name, address, overview 중 하나라도 keyword를 포함하면 검색된다.

            cb.or(...)를 사용했기 때문에 세 조건 중 하나만 만족해도 된다.
         */
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
        /*
            contentTypeIds가 null이거나 비어 있으면 조건을 추가하지 않는다.
         */
        if (condition.getContentTypeIds() == null || condition.getContentTypeIds().isEmpty()) {
            return;
        }

        /*
            contentTypeId가 요청으로 들어온 목록 중 하나에 포함되는지 검사한다.

            예:
            관광지, 문화시설, 음식점 등 타입 필터링에 사용 가능하다.
         */
        predicates.add(root.get("contentTypeId").in(condition.getContentTypeIds()));
    }

    /**
     * 접근성 카테고리 조건 추가
     */
    private static void addAccessibilityConditions(
            List<Predicate> predicates,
            Root<Attraction> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            AttractionSearchCondition condition
    ) {
        /*
            접근성 카테고리가 없으면 접근성 필터를 적용하지 않는다.
         */
        if (condition.getCategories() == null || condition.getCategories().isEmpty()) {
            return;
        }

        /*
            선택된 카테고리마다 EXISTS 조건을 추가한다 -> 여러 카테고리에 대해 모두 만족해야함
         */
        for (AccessibilityCategory category : condition.getCategories()) {
            predicates.add(hasAnyAccessibilityTypeInCategory(root, query, cb, category));
        }
    }

    /**
     * 특정 접근성 카테고리에 해당하는 접근성 정보가 하나라도 있는지 검사하는 조건 생성 -> 대분류에 대해서, 하나라도 만족하는 세부 카테고리가 있으면 됨
     */
    private static Predicate hasAnyAccessibilityTypeInCategory(
            Root<Attraction> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            AccessibilityCategory category
    ) {
        /*
            AccessibilityInfo를 대상으로 서브쿼리를 만든다.

            이 서브쿼리는 현재 조회 중인 관광지(root)에 대해
            조건에 맞는 AccessibilityInfo가 존재하는지 검사하기 위한 용도다.
         */
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AccessibilityInfo> accessibilityInfo = subquery.from(AccessibilityInfo.class);

        /*
            서브쿼리 조건 구성.

            cb.equal(accessibilityInfo.get("attraction"), root)
            - 현재 관광지와 연결된 접근성 정보만 찾는다.

            accessibilityInfo.get("type").in(category.getSearchTypes())
            - 해당 카테고리에 속하는 type인지 확인한다.
            - 예: PHYSICAL이면 주차, 출입구, 엘리베이터 등

            accessibilityInfo.get("status").in(DEFAULT_VALID_STATUSES)
            - AVAILABLE 또는 PARTIAL 상태만 유효한 접근성 정보로 본다.
         */
        subquery.select(accessibilityInfo.get("id"))
                .where(
                        cb.equal(accessibilityInfo.get("attraction"), root),
                        accessibilityInfo.get("type").in(category.getSearchTypes()),
                        accessibilityInfo.get("status").in(DEFAULT_VALID_STATUSES)
                );

        /*
            exists(subquery)를 반환한다.

            의미:
            "이 관광지에 대해 조건을 만족하는 AccessibilityInfo가 하나라도 존재하는가?"
         */
        return cb.exists(subquery);
    }

    /**
     * 문자열 값이 실제로 존재하는지 검사하는 유틸 메서드
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}