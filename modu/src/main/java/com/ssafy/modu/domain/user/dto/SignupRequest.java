package com.ssafy.modu.domain.user.dto;

import com.ssafy.modu.domain.user.enums.UiMode;
import jakarta.validation.constraints.*;

public record SignupRequest(

        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$")
        String userName,

        @NotBlank
        @Size(min = 8, max = 255)
        String password,

        @NotBlank
        @Size(max = 20)
        String nickname,

        @Min(0)
        @Max(6)
        Integer ageGroupCode,

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