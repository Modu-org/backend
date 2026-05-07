package com.ssafy.modu.domain.accessibility.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessibilitySource {
    KOR_WITH_DETAIL_WITH_TOUR(1),
    KOR_DETAIL_INFO(2),
    KOR_DETAIL_INTRO(3);

    private final int priority;
}
