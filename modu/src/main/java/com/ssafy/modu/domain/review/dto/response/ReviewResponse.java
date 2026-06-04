package com.ssafy.modu.domain.review.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.modu.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long reviewId;

    private Long userId;
    private String nickname;

    private Long attractionId;
    private String attractionName;

    private String content;

    @JsonProperty("isPrivate")
    private boolean privateReview;

    private int rate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getAttraction().getId(),
                review.getAttraction().getName(),
                review.getContent(),
                review.isPrivateReview(),
                review.getRate(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}