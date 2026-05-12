package com.ssafy.modu.domain.user.dto;

public record SignupResponse(
        Long userId,
        String userName,
        String nickname
) {
}