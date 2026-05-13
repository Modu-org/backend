package com.ssafy.modu.domain.user.service;

import com.ssafy.modu.domain.user.dto.LoginResponse;

public record LoginServiceResult(
        LoginResponse loginResponse,
        String refreshToken,
        long refreshTokenMaxAgeMillis
) {
}