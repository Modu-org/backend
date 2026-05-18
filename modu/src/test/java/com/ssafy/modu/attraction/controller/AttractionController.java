package com.ssafy.modu.attraction.controller;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import com.ssafy.modu.domain.attraction.controller.AttractionController;
import com.ssafy.modu.domain.attraction.dto.response.AttractionAccessibilityResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.service.AttractionService;
import com.ssafy.modu.global.auth.jwt.JwtTokenProvider;
import com.ssafy.modu.global.auth.redis.AccessTokenBlacklistRepository;
import com.ssafy.modu.global.auth.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 너의 실제 패키지에 맞춰서 controller 패키지 확인 필요
@WebMvcTest(AttractionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttractionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AttractionService attractionService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Test
    @DisplayName("관광지 상세 조회 성공 시 code 700과 상세 정보를 반환한다")
    void getAttractionDetail_success() throws Exception {
        // given
        Long attractionId = 1L;

        AttractionDetailResponse response = AttractionDetailResponse.builder()
                .attractionId(1L)
                .contentId("123456")
                .name("대구수목원")
                .address("대구광역시 달서구")
                .addressDetail("상세 주소")
                .zipcode("12345")
                .latitude(new BigDecimal("35.8012345"))
                .longitude(new BigDecimal("128.5123456"))
                .contentTypeId("12")
                .tel("053-000-0000")
                .firstImageUrl("https://example.com/image.jpg")
                .thumbnailImageUrl("https://example.com/thumb.jpg")
                .overview("관광지 설명입니다.")
                .homepage("https://example.com")
                .accessibility(List.of(
                        AttractionAccessibilityResponse.builder()
                                .category(AccessibilityCategory.PHYSICAL)
                                .type(AccessibilityType.WHEELCHAIR)
                                .status(AccessibilityStatus.AVAILABLE)
                                .description("휠체어 이용 가능")
                                .build(),
                        AttractionAccessibilityResponse.builder()
                                .category(AccessibilityCategory.INFANT_FAMILY)
                                .type(AccessibilityType.STROLLER)
                                .status(AccessibilityStatus.UNKNOWN)
                                .description("유모차 관련 정보 없음")
                                .build()
                ))
                .build();

        BDDMockito.given(attractionService.getAttractionDetail(eq(attractionId)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/attractions/{attractionId}", attractionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(700))
                .andExpect(jsonPath("$.message").value("관광지 상세 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.attractionId").value(1))
                .andExpect(jsonPath("$.data.contentId").value("123456"))
                .andExpect(jsonPath("$.data.name").value("대구수목원"))
                .andExpect(jsonPath("$.data.address").value("대구광역시 달서구"))
                .andExpect(jsonPath("$.data.addressDetail").value("상세 주소"))
                .andExpect(jsonPath("$.data.zipcode").value("12345"))
                .andExpect(jsonPath("$.data.latitude").value(35.8012345))
                .andExpect(jsonPath("$.data.longitude").value(128.5123456))
                .andExpect(jsonPath("$.data.contentTypeId").value("12"))
                .andExpect(jsonPath("$.data.tel").value("053-000-0000"))
                .andExpect(jsonPath("$.data.firstImageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.data.thumbnailImageUrl").value("https://example.com/thumb.jpg"))
                .andExpect(jsonPath("$.data.overview").value("관광지 설명입니다."))
                .andExpect(jsonPath("$.data.homepage").value("https://example.com"))
                .andExpect(jsonPath("$.data.accessibility", hasSize(2)))
                .andExpect(jsonPath("$.data.accessibility[0].category").value("PHYSICAL"))
                .andExpect(jsonPath("$.data.accessibility[0].type").value("WHEELCHAIR"))
                .andExpect(jsonPath("$.data.accessibility[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.accessibility[0].description").value("휠체어 이용 가능"))
                .andExpect(jsonPath("$.data.accessibility[1].category").value("INFANT_FAMILY"))
                .andExpect(jsonPath("$.data.accessibility[1].type").value("STROLLER"))
                .andExpect(jsonPath("$.data.accessibility[1].status").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.accessibility[1].description").value("유모차 관련 정보 없음"));
    }
}
