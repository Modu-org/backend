package com.ssafy.modu.domain.user.dto;

import com.ssafy.modu.domain.user.enums.UiMode;

public record LoginResponse(
        String accessToken,
        Long userId,
        String nickname,
        Integer ageGroupCode,
        Integer tripStyleCode,
        Boolean usesWheelchair,
        Boolean hasStroller,
        Boolean usesWalkingAid,
        Boolean hasServiceDog,
        Boolean needsVisualAssistance,
        UiMode uiMode
) {
}