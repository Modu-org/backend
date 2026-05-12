package com.ssafy.modu.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String userName,

        @NotBlank
        String password
) {
}