package com.ssafy.modu.domain.user.dto;


public record UserUpdateResponse(
        Long userId,
        String nickname
) {
}