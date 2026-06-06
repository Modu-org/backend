package com.ssafy.modu.domain.attraction.controller;

import com.ssafy.modu.domain.attraction.dto.request.AttractionSearchRequest;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionPageResponse;
import com.ssafy.modu.domain.attraction.service.AttractionService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attractions")
public class AttractionController {

    private final AttractionService attractionService;

    @GetMapping
    public ResponseEntity<ApiResponse<AttractionPageResponse>> searchAttractions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute AttractionSearchRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;

        Page<AttractionListResponse> page =
                attractionService.searchAttractions(userId, request);

        AttractionPageResponse data = AttractionPageResponse.from(page);

        if (page.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(SuccessCode.NO_CONTENT_DATA, "조건에 맞는 관광지가 없습니다.", data)
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "관광지 목록 조회에 성공했습니다.", data)
        );
    }
    @GetMapping("/{attractionId}")
    public ResponseEntity<ApiResponse<AttractionDetailResponse>> getAttractionDetail(
            @PathVariable Long attractionId
    ) {
        AttractionDetailResponse data = attractionService.getAttractionDetail(attractionId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "관광지 상세 조회에 성공했습니다.",
                        data
                )
        );
    }
}