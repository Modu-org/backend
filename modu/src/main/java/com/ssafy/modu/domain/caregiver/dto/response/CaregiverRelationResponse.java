package com.ssafy.modu.domain.caregiver.dto.response;

import com.ssafy.modu.domain.caregiver.entity.CaregiverRelation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CaregiverRelationResponse {

    private Long relationId;

    private Long travelerId;
    private String travelerName;

    private Long caregiverId;
    private String caregiverName;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;

    public static CaregiverRelationResponse from(
            CaregiverRelation relation,
            String travelerName,
            String caregiverName
    ) {
        return CaregiverRelationResponse.builder()
                .relationId(relation.getId())
                .travelerId(relation.getTravelerId())
                .travelerName(travelerName)
                .caregiverId(relation.getCaregiverId())
                .caregiverName(caregiverName)
                .active(relation.isActive())
                .createdAt(relation.getCreatedAt())
                .acceptedAt(relation.getAcceptedAt())
                .build();
    }
}