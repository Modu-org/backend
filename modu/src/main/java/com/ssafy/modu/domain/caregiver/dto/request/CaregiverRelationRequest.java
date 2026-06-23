package com.ssafy.modu.domain.caregiver.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CaregiverRelationRequest {

    @NotBlank(message = "보호자 사용자명은 필수입니다.")
    private String userName;
}