package com.ssafy.modu.global.util.mapper;

import com.ssafy.modu.domain.accessibility.entity.enums.*;

public record AccessibilityFieldMapping(
        AccessibilityCategory category,
        AccessibilityType type
) {
}
