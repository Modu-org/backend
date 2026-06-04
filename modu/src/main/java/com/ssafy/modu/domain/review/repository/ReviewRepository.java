package com.ssafy.modu.domain.review.repository;

import com.ssafy.modu.domain.review.entity.Review;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUser_IdAndAttraction_Id(Long userId, Long attractionId);

    @EntityGraph(attributePaths = {"user", "attraction"})
    @Query("""
        select r
        from Review r
        where r.attraction.id = :attractionId
          and (
              r.privateReview = false
              or (:userId is not null and r.user.id = :userId)
          )
    """)
    List<Review> findVisibleReviewsByAttractionId(
            @Param("attractionId") Long attractionId,
            @Param("userId") Long userId,
            Sort sort
    );

    @EntityGraph(attributePaths = {"user", "attraction"})
    List<Review> findByAttraction_IdAndUser_Id(
            Long attractionId,
            Long userId,
            Sort sort
    );

    @EntityGraph(attributePaths = {"user", "attraction"})
    List<Review> findByUser_Id(
            Long userId,
            Sort sort
    );

    @EntityGraph(attributePaths = {"user", "attraction"})
    Optional<Review> findByReviewId(Long reviewId);

    @EntityGraph(attributePaths = {"user", "attraction"})
    Optional<Review> findByReviewIdAndUser_Id(Long reviewId, Long userId);
}