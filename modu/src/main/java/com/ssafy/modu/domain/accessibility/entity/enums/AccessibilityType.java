package com.ssafy.modu.domain.accessibility.entity.enums;

import lombok.Getter;

@Getter
public enum AccessibilityType {

    PARKING(0),
    ACCESSIBLE_PARKING(1),
    PUBLIC_TRANSPORT(2),
    ROUTE(3),
    TICKET_OFFICE(4),
    WHEELCHAIR(5),
    EXIT(6),
    ELEVATOR(7),
    RESTROOM(8),
    ACCESSIBLE_RESTROOM(9),
    AUDITORIUM(10),
    ROOM(11),
    BRAILLE_BLOCK(12),
    HELP_DOG(13),
    GUIDE_HUMAN(14),
    AUDIO_GUIDE(15),
    BIG_PRINT(16),
    BRAILLE_PROMOTION(17),
    GUIDE_SYSTEM(18),
    SIGN_GUIDE(19),
    VIDEO_GUIDE(20),
    STROLLER(21),
    LACTATION_ROOM(22),
    BABY_SPARE_CHAIR(23),
    KIDS_FACILITY(24),
    PET(25),
    ETC(26);

    private final int code;

    AccessibilityType(int code) {
        this.code = code;
    }

    public static AccessibilityType fromCode(Integer code) {
        if (code == null) {
            return ETC;
        }

        for (AccessibilityType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        return ETC;
    }
}