package com.ssafy.modu.domain.user.dto;


public record LoginResponse(
        String accessToken,
        Long userId,
        String nickname,
        Boolean physical,
        Boolean infantFamily,
        Boolean visual,
        Boolean hearing
) {
}