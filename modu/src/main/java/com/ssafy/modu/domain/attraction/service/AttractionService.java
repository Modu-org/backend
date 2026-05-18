package com.ssafy.modu.domain.attraction.service;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.attraction.dto.condition.AttractionSearchCondition;
import com.ssafy.modu.domain.attraction.dto.request.AttractionSearchRequest;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.attraction.repository.specification.AttractionSpecification;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.UserDetailRepository;
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
import java.util.List;

import static com.ssafy.modu.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionService {

    /*
        기본 페이지 번호.
        Spring Data JPA의 페이지 번호는 0부터 시작한다.
        즉, page = 0이면 사용자 입장에서는 첫 번째 페이지를 의미한다.
     */
    private static final int DEFAULT_PAGE = 0;

    // 사용자가 size를 요청하지 않았을 때 기본으로 가져올 데이터 개수.
    private static final int DEFAULT_SIZE = 20;

    // 한 번에 너무 많은 데이터를 조회하지 못하도록 제한하는 최대 size.
    private static final int MAX_SIZE = 50;

    private final AttractionRepository attractionRepository;
    private final UserDetailRepository userDetailRepository;

    /**
     * 관광지 목록 조회 메서드
     *
     * 역할:
     * 1. userId로 사용자의 상세 정보를 조회한다.
     * 2. 사용자의 접근성 설정과 요청 파라미터를 바탕으로 필터링할 카테고리를 결정한다.
     * 3. 지역, 키워드, 콘텐츠 타입, 접근성 카테고리를 검색 조건으로 만든다.
     * 4. page, size 값을 바탕으로 Pageable을 만든다.
     * 5. Specification을 이용해서 조건에 맞는 관광지를 조회한다.
     * 6. Attraction 엔티티를 AttractionListResponse DTO로 변환해서 반환한다.
     */
    public Page<AttractionListResponse> searchAttractions(
            Long userId,
            AttractionSearchRequest request
    ) {
        /*
            사용자 상세 정보 조회.

            여기서 UserDetail을 가져오는 이유:
            - 요청에서 physical, visual, hearing, infantFamily 값이 직접 넘어오지 않을 수도 있다.
            - 그 경우 사용자가 회원가입/프로필 설정에서 저장해둔 접근성 정보를 기본값으로 사용하기 위해서다.

            예:
            요청에 physical이 없으면 userDetail.getPhysical() 값을 사용한다.
         */
        UserDetail userDetail = userDetailRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        /*
            실제 검색에 사용할 접근성 카테고리 목록을 만든다.

            예:
            physical = true  -> PHYSICAL 추가
            visual = true    -> VISUAL 추가
            hearing = false  -> HEARING 추가 안 함
         */
        List<AccessibilityCategory> categories = resolveCategories(userDetail, request);

        /*
            검색 조건 객체 생성.

            컨트롤러에서 받은 요청 DTO를 그대로 Repository에 넘기지 않고,
            검색에 필요한 값만 모아서 AttractionSearchCondition으로 만든다.

            이 조건 객체는 AttractionSpecification.search(condition)에서 사용된다.
         */
        AttractionSearchCondition condition = AttractionSearchCondition.builder()
                .regionCode(request.getRegionCode())              // 지역 코드
                .sigunguCode(request.getSigunguCode())            // 시군구 코드
                .keyword(request.getKeyword())                    // 검색 키워드
                .contentTypeIds(request.getContentTypeIds())      // 관광지 타입 목록
                .categories(categories)                           // 접근성 카테고리 목록
                .build();

        /*
            페이지 정보 생성.

            request의 page, size 값을 바탕으로 Pageable을 만든다.
            page나 size가 잘못 들어온 경우 기본값 또는 제한값으로 보정한다.
         */
        Pageable pageable = createPageable(request);

        /*
            Specification을 사용해서 동적 검색을 수행한다.

            AttractionSpecification.search(condition):
            - regionCode가 있으면 지역 조건 추가
            - sigunguCode가 있으면 시군구 조건 추가
            - keyword가 있으면 이름/주소 검색 조건 추가
            - contentTypeIds가 있으면 관광지 타입 조건 추가
            - categories가 있으면 접근성 조건 추가

            pageable:
            - 몇 번째 페이지를 조회할지
            - 한 페이지에 몇 개를 조회할지
            - 어떤 기준으로 정렬할지
         */
        Page<Attraction> attractions = attractionRepository.findAll(
                AttractionSpecification.search(condition),
                pageable
        );

        /*
            Page<Attraction>을 Page<AttractionListResponse>로 변환한다.

            map()을 사용하면 Page의 페이지 정보는 유지하면서,
            content 안의 Attraction만 AttractionListResponse로 바뀐다.

            즉,
            Attraction 엔티티 전체를 프론트에 주는 것이 아니라
            목록 화면에 필요한 필드만 담은 DTO로 변환해서 반환한다.
         */
        return attractions.map(AttractionListResponse::from);
    }

    /**
     * 관광지 상세 조회 메서드
     *
     * 역할:
     * 1. attractionId로 관광지를 조회한다.
     * 2. 무장애 정보도 함께 fetch join해서 가져온다.
     * 3. 관광지가 없으면 ATTRACTION_NOT_FOUND 예외를 발생시킨다.
     * 4. AttractionDetailResponse로 변환해서 반환한다.
     */
    public AttractionDetailResponse getAttractionDetail(Long attractionId) {
        /*
            관광지 상세 조회.

            findWithAccessibilityInfosByIdAndShowFlagTrue:
            - attractionId가 일치하고
            - showFlag가 true인 관광지만 조회하고
            - accessibilityInfos도 함께 가져오는 메서드로 보인다.

            이렇게 하는 이유:
            상세 조회에서는 관광지 기본 정보뿐만 아니라
            무장애 정보 목록도 같이 필요하기 때문이다.
         */
        Attraction attraction = attractionRepository.findWithAccessibilityInfosByIdAndShowFlagTrue(attractionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND));

        /*
            Attraction 엔티티를 상세 조회 응답 DTO로 변환한다.

            AttractionDetailResponse 안에는
            관광지 이름, 주소, 전화번호, 이미지, 설명, 홈페이지,
            그리고 accessibility 정보까지 포함된다.
         */
        return AttractionDetailResponse.from(attraction);
    }

    /**
     * 요청값과 사용자 기본 설정을 바탕으로 접근성 카테고리 목록을 만드는 메서드
     *
     * 핵심 로직:
     * - request에 값이 있으면 request 값을 우선 사용한다.
     * - request에 값이 없으면 userDetail에 저장된 사용자 기본 설정을 사용한다.
     *
     * 예:
     * request.physical = true이면 PHYSICAL 추가
     * request.physical = null이면 userDetail.physical 값을 보고 결정
     */
    private List<AccessibilityCategory> resolveCategories(
            UserDetail userDetail,
            AttractionSearchRequest request
    ) {
        List<AccessibilityCategory> result = new ArrayList<>();

        /*
            지체장애/이동약자 관련 필터 여부 결정.

            request.getPhysical() 값이 null이 아니면 요청값 사용.
            null이면 userDetail.getPhysical() 값 사용.
         */
        boolean physical = request.getPhysical() != null
                ? request.getPhysical()
                : Boolean.TRUE.equals(userDetail.getPhysical());

        /*
            영유아 가족 관련 필터 여부 결정.
         */
        boolean infantFamily = request.getInfantFamily() != null
                ? request.getInfantFamily()
                : Boolean.TRUE.equals(userDetail.getInfantFamily());

        /*
            시각장애 관련 필터 여부 결정.
         */
        boolean visual = request.getVisual() != null
                ? request.getVisual()
                : Boolean.TRUE.equals(userDetail.getVisual());

        /*
            청각장애 관련 필터 여부 결정.
         */
        boolean hearing = request.getHearing() != null
                ? request.getHearing()
                : Boolean.TRUE.equals(userDetail.getHearing());

        /*
            physical이 true이면 검색 조건에 PHYSICAL 카테고리를 추가한다.
         */
        if (physical) {
            result.add(AccessibilityCategory.PHYSICAL);
        }

        /*
            infantFamily가 true이면 검색 조건에 INFANT_FAMILY 카테고리를 추가한다.
         */
        if (infantFamily) {
            result.add(AccessibilityCategory.INFANT_FAMILY);
        }

        /*
            visual이 true이면 검색 조건에 VISUAL 카테고리를 추가한다.
         */
        if (visual) {
            result.add(AccessibilityCategory.VISUAL);
        }

        /*
            hearing이 true이면 검색 조건에 HEARING 카테고리를 추가한다.
         */
        if (hearing) {
            result.add(AccessibilityCategory.HEARING);
        }

        /*
            최종적으로 검색에 사용할 접근성 카테고리 목록 반환.
         */
        return result;
    }

    /**
     * 페이지 요청 정보를 만드는 메서드
     *
     * 역할:
     * 1. page 값이 없거나 음수면 기본값 0 사용
     * 2. size 값이 없으면 기본값 20 사용
     * 3. size가 1보다 작으면 1로 보정
     * 4. size가 50보다 크면 50으로 제한
     * 5. apiModifiedTime 기준 내림차순 정렬
     */
    private Pageable createPageable(AttractionSearchRequest request) {
        /*
            page 결정.

            page가 null이거나 0보다 작으면 DEFAULT_PAGE 사용.
            즉, 잘못된 페이지 요청은 첫 번째 페이지로 처리한다.
         */
        int page = request.getPage() == null || request.getPage() < 0
                ? DEFAULT_PAGE
                : request.getPage();

        /*
            size 결정.

            size가 null이면 DEFAULT_SIZE 사용.
            size가 1보다 작으면 1로 보정.
            size가 MAX_SIZE보다 크면 MAX_SIZE로 제한.

            예:
            size = null  -> 20
            size = -10   -> 1
            size = 0     -> 1
            size = 30    -> 30
            size = 100   -> 50
         */
        int size = request.getSize() == null
                ? DEFAULT_SIZE
                : Math.min(Math.max(request.getSize(), 1), MAX_SIZE);

        /*
            PageRequest 생성.

            Sort.by(Sort.Direction.DESC, "apiModifiedTime"):
            apiModifiedTime이 최신인 관광지가 먼저 나오도록 정렬한다.
         */
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "apiModifiedTime")
        );
    }
}