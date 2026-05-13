package com.ssafy.modu.domain.accessibility.entity.enums;

import lombok.Getter;

@Getter
public enum AccessibilityCategory {

    PHYSICAL(0),
    VISUAL(1),
    HEARING(2),
    INFANT_FAMILY(3),
    COMMON(4),
    ETC(5);

    private final int code;

    AccessibilityCategory(int code) {
        this.code = code;
    }

    public static AccessibilityCategory fromCode(Integer code) {
        if (code == null) {
            return ETC;
        }

        for (AccessibilityCategory category : values()) {
            if (category.code == code) {
                return category;
            }
        }

        return ETC;
    }
}