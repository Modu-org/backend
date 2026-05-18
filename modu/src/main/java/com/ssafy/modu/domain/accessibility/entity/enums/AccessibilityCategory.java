package com.ssafy.modu.domain.accessibility.entity.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum AccessibilityCategory {

    PHYSICAL(0, List.of(
            AccessibilityType.ACCESSIBLE_PARKING,
            AccessibilityType.PARKING,
            AccessibilityType.PUBLIC_TRANSPORT,
            AccessibilityType.ROUTE,
            AccessibilityType.TICKET_OFFICE,
            AccessibilityType.WHEELCHAIR,
            AccessibilityType.EXIT,
            AccessibilityType.ELEVATOR,
            AccessibilityType.RESTROOM,
            AccessibilityType.ACCESSIBLE_RESTROOM,
            AccessibilityType.AUDITORIUM,
            AccessibilityType.ROOM
    )),

    VISUAL(1, List.of(
            AccessibilityType.ACCESSIBLE_PARKING,
            AccessibilityType.PARKING,
            AccessibilityType.PUBLIC_TRANSPORT,
            AccessibilityType.ROUTE,
            AccessibilityType.TICKET_OFFICE,
            AccessibilityType.RESTROOM,
            AccessibilityType.BRAILLE_BLOCK,
            AccessibilityType.HELP_DOG,
            AccessibilityType.GUIDE_HUMAN,
            AccessibilityType.AUDIO_GUIDE,
            AccessibilityType.BIG_PRINT,
            AccessibilityType.BRAILLE_PROMOTION,
            AccessibilityType.GUIDE_SYSTEM
    )),

    HEARING(2, List.of(
            AccessibilityType.ACCESSIBLE_PARKING,
            AccessibilityType.PARKING,
            AccessibilityType.PUBLIC_TRANSPORT,
            AccessibilityType.ROUTE,
            AccessibilityType.TICKET_OFFICE,
            AccessibilityType.RESTROOM,
            AccessibilityType.GUIDE_HUMAN,
            AccessibilityType.GUIDE_SYSTEM,
            AccessibilityType.SIGN_GUIDE,
            AccessibilityType.VIDEO_GUIDE,
            AccessibilityType.ROOM
    )),

    INFANT_FAMILY(3, List.of(
            AccessibilityType.PARKING,
            AccessibilityType.PUBLIC_TRANSPORT,
            AccessibilityType.ROUTE,
            AccessibilityType.RESTROOM,
            AccessibilityType.STROLLER,
            AccessibilityType.LACTATION_ROOM,
            AccessibilityType.BABY_SPARE_CHAIR,
            AccessibilityType.KIDS_FACILITY
    )),

    COMMON(4, List.of(
            AccessibilityType.ACCESSIBLE_PARKING,
            AccessibilityType.PARKING,
            AccessibilityType.PUBLIC_TRANSPORT,
            AccessibilityType.ROUTE,
            AccessibilityType.RESTROOM
    )),

    ETC(5, List.of(
            AccessibilityType.ETC
    ));

    private final int code;
    private final List<AccessibilityType> searchTypes;

    AccessibilityCategory(int code, List<AccessibilityType> searchTypes) {
        this.code = code;
        this.searchTypes = searchTypes;
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