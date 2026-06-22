package com.ssafy.modu.user.service;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilitySource;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.attraction.service.AttractionService;
import com.ssafy.modu.domain.user.repository.UserDetailRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AttractionServiceTest {

    @Mock
    private AttractionRepository attractionRepository;

    @Mock
    private UserDetailRepository userDetailRepository;

    @InjectMocks
    private AttractionService attractionService;

    @Test
    @DisplayName("관광지 상세 조회 성공 시 상세 정보와 접근성 정보를 반환한다")
    void getAttractionDetail_success() {
        // given
        Long attractionId = 1L;

        Attraction attraction = createAttraction();

        AccessibilityInfo wheelchairInfo = AccessibilityInfo.create(
                attraction,
                anyAccessibilitySource(),
                "wheelchair",
                "휠체어 이용 가능",
                AccessibilityCategory.PHYSICAL,
                AccessibilityType.WHEELCHAIR,
                AccessibilityStatus.AVAILABLE
        );

        AccessibilityInfo strollerInfo = AccessibilityInfo.create(
                attraction,
                anyAccessibilitySource(),
                "stroller",
                "유모차 관련 정보 없음",
                AccessibilityCategory.INFANT_FAMILY,
                AccessibilityType.STROLLER,
                AccessibilityStatus.UNKNOWN
        );

        List<AccessibilityInfo> accessibilityInfos = new ArrayList<>();
        accessibilityInfos.add(wheelchairInfo);
        accessibilityInfos.add(strollerInfo);

        ReflectionTestUtils.setField(attraction, "accessibilityInfos", accessibilityInfos);

        given(attractionRepository.findWithAccessibilityInfosByIdAndShowFlagTrueAndApiRemovedFalse(attractionId))
                .willReturn(Optional.of(attraction));

        // when
        AttractionDetailResponse response = attractionService.getAttractionDetail(attractionId);

        // then
        assertThat(response.getAttractionId()).isEqualTo(1L);
        assertThat(response.getContentId()).isEqualTo("123456");
        assertThat(response.getName()).isEqualTo("대구수목원");
        assertThat(response.getAddress()).isEqualTo("대구광역시 달서구");
        assertThat(response.getAddressDetail()).isEqualTo("상세 주소");
        assertThat(response.getZipcode()).isEqualTo("12345");
        assertThat(response.getLatitude()).isEqualByComparingTo("35.8012345");
        assertThat(response.getLongitude()).isEqualByComparingTo("128.5123456");
        assertThat(response.getContentTypeId()).isEqualTo("12");
        assertThat(response.getTel()).isEqualTo("053-000-0000");
        assertThat(response.getFirstImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(response.getThumbnailImageUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(response.getOverview()).isEqualTo("관광지 설명입니다.");
        assertThat(response.getHomepage()).isEqualTo("https://example.com");

        assertThat(response.getAccessibility()).hasSize(2);

        assertThat(response.getAccessibility())
                .extracting("category")
                .containsExactlyInAnyOrder(
                        AccessibilityCategory.PHYSICAL,
                        AccessibilityCategory.INFANT_FAMILY
                );

        assertThat(response.getAccessibility())
                .extracting("type")
                .containsExactlyInAnyOrder(
                        AccessibilityType.WHEELCHAIR,
                        AccessibilityType.STROLLER
                );

        assertThat(response.getAccessibility())
                .extracting("status")
                .containsExactlyInAnyOrder(
                        AccessibilityStatus.AVAILABLE,
                        AccessibilityStatus.UNKNOWN
                );

        assertThat(response.getAccessibility())
                .extracting("description")
                .containsExactlyInAnyOrder(
                        "휠체어 이용 가능",
                        "유모차 관련 정보 없음"
                );
    }

    @Test
    @DisplayName("존재하지 않는 관광지 상세 조회 시 ATTRACTION_NOT_FOUND 예외가 발생한다")
    void getAttractionDetail_notFound() {
        // given
        Long attractionId = 999L;

        given(attractionRepository.findWithAccessibilityInfosByIdAndShowFlagTrueAndApiRemovedFalse(attractionId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attractionService.getAttractionDetail(attractionId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTRACTION_NOT_FOUND);
    }

    private Attraction createAttraction() {
        try {
            Constructor<Attraction> constructor = Attraction.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Attraction attraction = constructor.newInstance();

            ReflectionTestUtils.setField(attraction, "id", 1L);
            ReflectionTestUtils.setField(attraction, "contentId", "123456");
            ReflectionTestUtils.setField(attraction, "name", "대구수목원");
            ReflectionTestUtils.setField(attraction, "address", "대구광역시 달서구");
            ReflectionTestUtils.setField(attraction, "addressDetail", "상세 주소");
            ReflectionTestUtils.setField(attraction, "zipcode", "12345");
            ReflectionTestUtils.setField(attraction, "latitude", new BigDecimal("35.8012345"));
            ReflectionTestUtils.setField(attraction, "longitude", new BigDecimal("128.5123456"));
            ReflectionTestUtils.setField(attraction, "contentTypeId", "12");
            ReflectionTestUtils.setField(attraction, "tel", "053-000-0000");
            ReflectionTestUtils.setField(attraction, "firstImageUrl", "https://example.com/image.jpg");
            ReflectionTestUtils.setField(attraction, "thumbnailImageUrl", "https://example.com/thumb.jpg");
            ReflectionTestUtils.setField(attraction, "overview", "관광지 설명입니다.");
            ReflectionTestUtils.setField(attraction, "homepage", "https://example.com");

            return attraction;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AccessibilitySource anyAccessibilitySource() {
        return AccessibilitySource.values()[0];
    }
}

