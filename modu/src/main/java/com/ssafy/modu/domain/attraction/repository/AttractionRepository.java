package com.ssafy.modu.domain.attraction.repository;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.entity.enums.TourDetailLoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    Optional<Attraction> findByContentId(String contentId);

    boolean existsByContentId(String contentId);

    List<Attraction> findByContentTypeId(String contentTypeId);

    /**
     * 무장애 후보(목록에 등장) 중, 아직 detailWithTour2를 호출하지 않은 대상 조회.
     * NOT_STARTED만 대상으로 잡아야 NO_DATA/SUCCESS가 반복 호출되지 않는다.
     */
    List<Attraction> findByAccessibleCandidateTrueAndAccessibilityStatusOrderByApiModifiedTimeDesc(
            TourDetailLoadStatus accessibilityStatus,
            Pageable pageable
    );

    /**
     * 무장애 후보(목록에 등장) 중, 아직 detailCommon2를 호출하지 않은 대상 조회.
     * NOT_STARTED만 대상으로 잡아야 NO_DATA/SUCCESS가 반복 호출되지 않는다.
     */
    List<Attraction> findByAccessibleCandidateTrueAndCommonDetailStatusOrderByApiModifiedTimeDesc(
            TourDetailLoadStatus commonDetailStatus,
            Pageable pageable
    );
}
