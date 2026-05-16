package com.ssafy.modu.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @NotBlank
        @Size(max = 20)
        String nickname,

        @Size(max = 255)
        String profileImg,

        Boolean physical,

        Boolean infantFamily,

        Boolean visual,

        Boolean hearing
) {
}