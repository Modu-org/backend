package com.ssafy.modu.domain.review.controller;

import com.ssafy.modu.domain.review.dto.request.ReviewCreateRequest;
import com.ssafy.modu.domain.review.dto.request.ReviewUpdateRequest;
import com.ssafy.modu.domain.review.dto.response.ReviewResponse;
import com.ssafy.modu.domain.review.entity.enums.ReviewSortType;
import com.ssafy.modu.domain.review.service.ReviewService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/attractions/{attractionId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long attractionId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewResponse response = reviewService.createReview(
                userDetails.getUserId(),
                attractionId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.CREATED, "리뷰가 작성되었습니다.", response)
        );
    }

    @GetMapping("/attractions/{attractionId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAttractionReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long attractionId,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sort
    ) {
        Long userId = userDetails == null ? null : userDetails.getUserId();

        List<ReviewResponse> response = reviewService.getAttractionReviews(
                userId,
                attractionId,
                mineOnly,
                sort
        );

        if (response.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            SuccessCode.NO_CONTENT_DATA,
                            mineOnly ? "작성한 리뷰가 없습니다." : "등록된 리뷰가 없습니다.",
                            response
                    )
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        mineOnly ? "내 리뷰 목록 조회에 성공했습니다." : "관광지 리뷰 목록 조회에 성공했습니다.",
                        response
                )
        );
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        ReviewResponse response = reviewService.getReview(userDetails.getUserId(), reviewId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "리뷰 조회에 성공했습니다.", response)
        );
    }

    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(
                userDetails.getUserId(),
                reviewId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "리뷰가 수정되었습니다.", response)
        );
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(userDetails.getUserId(), reviewId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "리뷰가 삭제되었습니다.", null)
        );
    }

    @GetMapping("/users/me/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sort
    ) {
        List<ReviewResponse> response = reviewService.getMyReviews(
                userDetails.getUserId(),
                sort
        );

        if (response.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            SuccessCode.NO_CONTENT_DATA,
                            "작성한 리뷰가 없습니다.",
                            response
                    )
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "내 리뷰 목록 조회에 성공했습니다.",
                        response
                )
        );
    }
}