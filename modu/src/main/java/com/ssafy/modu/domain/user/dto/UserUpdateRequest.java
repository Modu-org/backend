package com.ssafy.modu.domain.user.dto;

import com.ssafy.modu.domain.user.enums.UiMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @NotBlank
        @Size(max = 20)
        String nickname,

        @Min(0)
        @Max(6)
        Integer ageGroupCode,
        @Size(max = 255)
        String profileImg,
        @Min(1)
        @Max(5)
        Integer tripStyleCode,

        Boolean usesWheelchair,
        Boolean hasStroller,
        Boolean usesWalkingAid,
        Boolean hasServiceDog,
        Boolean needsVisualAssistance,

        UiMode uiMode
) {
}