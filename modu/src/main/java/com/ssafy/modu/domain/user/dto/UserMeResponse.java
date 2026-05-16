package com.ssafy.modu.domain.user.dto;


public record UserMeResponse(
        Long userId,
        String userName,
        String nickname,
        String profileImg,
        Boolean physical,
        Boolean infantFamily,
        Boolean visual,
        Boolean hearing
) {
}