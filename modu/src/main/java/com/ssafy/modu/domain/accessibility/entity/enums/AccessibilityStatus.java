package com.ssafy.modu.domain.accessibility.entity.enums;

import lombok.Getter;

@Getter
public enum AccessibilityStatus {

    AVAILABLE(0),
    UNAVAILABLE(1),
    PARTIAL(2),
    UNKNOWN(3),
    NEED_CHECK(4);

    private final int code;

    AccessibilityStatus(int code) {
        this.code = code;
    }

    public static AccessibilityStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }

        for (AccessibilityStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        return UNKNOWN;
    }
}