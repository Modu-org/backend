package com.ssafy.modu.domain.caregiver.service;

import com.ssafy.modu.domain.caregiver.dto.request.CaregiverRelationRequest;
import com.ssafy.modu.domain.caregiver.dto.response.CaregiverRelationResponse;
import com.ssafy.modu.domain.caregiver.entity.CaregiverRelation;
import com.ssafy.modu.domain.caregiver.repository.CaregiverRelationRepository;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverRelationService {

    private final CaregiverRelationRepository caregiverRelationRepository;
    private final UserRepository userRepository;

    @Transactional
    public CaregiverRelationResponse addCaregiver(
            Long travelerId,
            CaregiverRelationRequest request
    ) {
        Long caregiverId = request.getCaregiverId();

        if (travelerId.equals(caregiverId)) {
            throw new BusinessException(ErrorCode.INVALID_CAREGIVER_RELATION_REQUEST);
        }

        if (!userRepository.existsById(caregiverId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        CaregiverRelation relation = caregiverRelationRepository
                .findByTravelerIdAndCaregiverId(travelerId, caregiverId)
                .map(existingRelation -> {
                    existingRelation.activate();
                    return existingRelation;
                })
                .orElseGet(() -> caregiverRelationRepository.save(
                        CaregiverRelation.create(travelerId, caregiverId)
                ));

        return CaregiverRelationResponse.from(relation);
    }

    @Transactional
    public void removeCaregiver(
            Long travelerId,
            Long caregiverId
    ) {
        CaregiverRelation relation = caregiverRelationRepository
                .findByTravelerIdAndCaregiverId(travelerId, caregiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_RELATION_NOT_FOUND));

        relation.deactivate();
    }

    public List<CaregiverRelationResponse> getMyCaregivers(Long travelerId) {
        return caregiverRelationRepository.findAllByTravelerIdAndActiveTrue(travelerId)
                .stream()
                .map(CaregiverRelationResponse::from)
                .toList();
    }
}