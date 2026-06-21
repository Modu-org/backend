package com.ssafy.modu.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenSaveRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String token;
}