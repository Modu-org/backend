package com.ssafy.modu.domain.caregiver.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CaregiverRelationRequest {

    @NotNull(message = "보호자 ID는 필수입니다.")
    private String userName;
}