package com.ssafy.modu.domain.attraction.dto.response;

import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttractionAccessibilityResponse {

    private AccessibilityCategory category;
    private AccessibilityType type;
    private AccessibilityStatus status;
    private String description;

    public static AttractionAccessibilityResponse from(AccessibilityInfo accessibilityInfo) {
        return AttractionAccessibilityResponse.builder()
                .category(accessibilityInfo.getCategory())
                .type(accessibilityInfo.getType())
                .status(accessibilityInfo.getStatus())
                .description(accessibilityInfo.getRawValue())
                .build();
    }
}