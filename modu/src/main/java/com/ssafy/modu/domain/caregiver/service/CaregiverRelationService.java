package com.ssafy.modu.domain.caregiver.service;

import com.ssafy.modu.domain.caregiver.dto.request.CaregiverRelationRequest;
import com.ssafy.modu.domain.caregiver.dto.response.CaregiverRelationResponse;
import com.ssafy.modu.domain.caregiver.entity.CaregiverRelation;
import com.ssafy.modu.domain.caregiver.repository.CaregiverRelationRepository;
import com.ssafy.modu.domain.notification.entity.enums.NotificationType;
import com.ssafy.modu.domain.notification.service.NotificationService;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverRelationService {

    private final CaregiverRelationRepository caregiverRelationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CaregiverRelationResponse requestCaregiver(
            Long travelerId,
            CaregiverRelationRequest request
    ) {
        User caregiver = userRepository.findByUserNameAndIsDeletedFalse(request.getUserName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Long caregiverId = caregiver.getId();

        if (travelerId.equals(caregiverId)) {
            throw new BusinessException(ErrorCode.INVALID_CAREGIVER_RELATION_REQUEST);
        }

        Optional<CaregiverRelation> relationOptional =
                caregiverRelationRepository.findByTravelerIdAndCaregiverId(
                        travelerId,
                        caregiverId
                );

        if (relationOptional.isPresent()) {
            CaregiverRelation relation = relationOptional.get();

            if (relation.isActive()) {
                throw new BusinessException(ErrorCode.CAREGIVER_RELATION_ALREADY_ACCEPTED);
            }

            if (relation.getAcceptedAt() == null) {
                throw new BusinessException(ErrorCode.CAREGIVER_RELATION_ALREADY_REQUESTED);
            }

            relation.requestAgain();

            notificationService.createNotification(
                    caregiverId,
                    NotificationType.CAREGIVER_REQUEST,
                    "보호자 등록 요청",
                    "새 보호자 등록 요청이 도착했습니다.",
                    relation.getId()
            );

            return toResponse(relation);
        }

        CaregiverRelation relation = caregiverRelationRepository.save(
                CaregiverRelation.createPending(travelerId, caregiverId)
        );

        notificationService.createNotification(
                caregiverId,
                NotificationType.CAREGIVER_REQUEST,
                "보호자 등록 요청",
                "새 보호자 등록 요청이 도착했습니다.",
                relation.getId()
        );

        return toResponse(relation);
    }

    @Transactional
    public void acceptCaregiverRequest(
            Long caregiverId,
            Long relationId
    ) {
        CaregiverRelation relation = caregiverRelationRepository
                .findByIdAndCaregiverId(relationId, caregiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_RELATION_NOT_FOUND));

        validatePendingRequest(relation);

        relation.accept();

        notificationService.createNotification(
                relation.getTravelerId(),
                NotificationType.CAREGIVER_ACCEPTED,
                "보호자 등록 수락",
                "보호자 등록 요청이 수락되었습니다.",
                relation.getId()
        );
    }

    @Transactional
    public void rejectCaregiverRequest(
            Long caregiverId,
            Long relationId
    ) {
        CaregiverRelation relation = caregiverRelationRepository
                .findByIdAndCaregiverId(relationId, caregiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_RELATION_NOT_FOUND));

        validatePendingRequest(relation);

        caregiverRelationRepository.delete(relation);
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
        List<CaregiverRelation> relations =
                caregiverRelationRepository.findAllByTravelerIdAndActiveTrue(travelerId);

        return toResponses(relations);
    }

    public List<CaregiverRelationResponse> getReceivedRequests(Long caregiverId) {
        List<CaregiverRelation> relations =
                caregiverRelationRepository.findAllByCaregiverIdAndActiveFalseAndAcceptedAtIsNull(
                        caregiverId
                );

        return toResponses(relations);
    }

    private void validatePendingRequest(CaregiverRelation relation) {
        if (relation.isActive() || relation.getAcceptedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_CAREGIVER_RELATION_REQUEST);
        }
    }

    private CaregiverRelationResponse toResponse(CaregiverRelation relation) {
        Set<Long> userIds = Set.of(
                relation.getTravelerId(),
                relation.getCaregiverId()
        );

        Map<Long, String> userNameMap = userRepository.findAllByIdInAndIsDeletedFalse(userIds)
                .stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getUserName
                ));

        return CaregiverRelationResponse.from(
                relation,
                userNameMap.get(relation.getTravelerId()),
                userNameMap.get(relation.getCaregiverId())
        );
    }

    private List<CaregiverRelationResponse> toResponses(List<CaregiverRelation> relations) {
        if (relations.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = relations.stream()
                .flatMap(relation -> List.of(
                        relation.getTravelerId(),
                        relation.getCaregiverId()
                ).stream())
                .collect(Collectors.toSet());

        Map<Long, String> userNameMap = userRepository.findAllByIdInAndIsDeletedFalse(userIds)
                .stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getUserName
                ));

        return relations.stream()
                .map(relation -> CaregiverRelationResponse.from(
                        relation,
                        userNameMap.get(relation.getTravelerId()),
                        userNameMap.get(relation.getCaregiverId())
                ))
                .toList();
    }
}