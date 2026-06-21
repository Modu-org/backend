package com.ssafy.modu.domain.caregiver.repository;

import com.ssafy.modu.domain.caregiver.entity.CaregiverRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaregiverRelationRepository extends JpaRepository<CaregiverRelation, Long> {

    Optional<CaregiverRelation> findByTravelerIdAndCaregiverId(
            Long travelerId,
            Long caregiverId
    );

    boolean existsByTravelerIdAndCaregiverIdAndActiveTrue(
            Long travelerId,
            Long caregiverId
    );

    List<CaregiverRelation> findAllByTravelerIdAndActiveTrue(
            Long travelerId
    );

    List<CaregiverRelation> findAllByCaregiverIdAndActiveTrue(
            Long caregiverId
    );
}