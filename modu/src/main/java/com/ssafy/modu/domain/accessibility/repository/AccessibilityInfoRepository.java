package com.ssafy.modu.domain.accessibility.repository;

import com.ssafy.modu.domain.accessibility.entity.*;
import com.ssafy.modu.domain.accessibility.entity.enums.*;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessibilityInfoRepository extends JpaRepository<AccessibilityInfo, Long> {

    Optional<AccessibilityInfo> findByAttractionAndSourceAndSourceField(
            Attraction attraction,
            AccessibilitySource source,
            String sourceField
    );

    List<AccessibilityInfo> findByAttraction_Id(Long attractionId);

    List<AccessibilityInfo> findByTypeAndStatus(AccessibilityType type, AccessibilityStatus status);

    List<AccessibilityInfo> findByCategoryAndStatus(AccessibilityCategory category, AccessibilityStatus status);
}
