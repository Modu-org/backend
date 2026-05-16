package com.ssafy.modu.domain.user.dto;


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

        Boolean physical,

        Boolean infantFamily,

        Boolean visual,

        Boolean hearing

) {
}