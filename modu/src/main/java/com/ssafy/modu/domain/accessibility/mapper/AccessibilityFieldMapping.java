package com.ssafy.modu.domain.accessibility.mapper;

import com.ssafy.modu.domain.accessibility.entity.enums.*;

public record AccessibilityFieldMapping(
        AccessibilityCategory category,
        AccessibilityType type
) {
}
