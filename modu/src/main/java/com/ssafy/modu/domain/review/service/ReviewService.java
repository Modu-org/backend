package com.ssafy.modu.domain.review.service;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.review.dto.request.ReviewCreateRequest;
import com.ssafy.modu.domain.review.dto.request.ReviewUpdateRequest;
import com.ssafy.modu.domain.review.dto.response.ReviewResponse;
import com.ssafy.modu.domain.review.entity.Review;
import com.ssafy.modu.domain.review.entity.enums.ReviewSortType;
import com.ssafy.modu.domain.review.repository.ReviewRepository;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AttractionRepository attractionRepository;

    public ReviewResponse createReview(Long userId, Long attractionId, ReviewCreateRequest request) {
        if (reviewRepository.existsByUser_IdAndAttraction_Id(userId, attractionId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND));

        Review review = new Review(
                user,
                attraction,
                request.getContent(),
                request.isPrivateReview(),
                request.getRate()
        );

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAttractionReviews(
            Long userId,
            Long attractionId,
            boolean mineOnly,
            ReviewSortType sortType
    ) {
        if (!attractionRepository.existsById(attractionId)) {
            throw new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND);
        }

        Sort sort = toSort(sortType);

        if (mineOnly) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            return reviewRepository.findByAttraction_IdAndUser_Id(
                            attractionId,
                            userId,
                            sort
                    )
                    .stream()
                    .map(ReviewResponse::from)
                    .toList();
        }

        return reviewRepository.findVisibleReviewsByAttractionId(
                        attractionId,
                        userId,
                        sort
                )
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.isPrivateReview() && !review.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_ACCESS_DENIED);
        }

        return ReviewResponse.from(review);
    }

    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_ACCESS_DENIED);
        }

        review.update(
                request.getContent(),
                request.isPrivateReview(),
                request.getRate()
        );

        return ReviewResponse.from(review);
    }

    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_ACCESS_DENIED);
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long userId, ReviewSortType sortType) {
        Sort sort = toSort(sortType);

        return reviewRepository.findByUser_Id(userId, sort)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    private Sort toSort(ReviewSortType sortType) {
        if (sortType == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case RATE_DESC -> Sort.by(Sort.Direction.DESC, "rate")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case RATE_ASC -> Sort.by(Sort.Direction.ASC, "rate")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        };
    }
}