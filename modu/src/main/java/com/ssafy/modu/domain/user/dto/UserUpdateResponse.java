package com.ssafy.modu.domain.user.dto;

import com.ssafy.modu.domain.user.enums.UiMode;

public record UserUpdateResponse(
        Long userId,
        String nickname,
        UiMode uiMode
) {
}