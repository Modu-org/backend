package com.ssafy.modu.domain.user.dto;

import com.ssafy.modu.domain.user.enums.UiMode;

public record UserMeResponse(
        Long userId,
        String userName,
        String nickname,
        String profileImg,
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