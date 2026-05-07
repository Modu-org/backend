package com.ssafy.modu.global.util.mapper;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccessibilityFieldMapper {

    /**
     * detailWithTour2(source_field) → (category, type) 매핑.
     * status는 raw_value를 보고 AccessibilityStatusParser가 별도로 파싱한다.
     */
    private static final Map<String, AccessibilityFieldMapping> WITH_TOUR_FIELD_MAP = Map.ofEntries(
            entry("parking", AccessibilityCategory.PHYSICAL, AccessibilityType.ACCESSIBLE_PARKING),
            entry("publictransport", AccessibilityCategory.PHYSICAL, AccessibilityType.PUBLIC_TRANSPORT),
            entry("route", AccessibilityCategory.PHYSICAL, AccessibilityType.ROUTE),
            entry("ticketoffice", AccessibilityCategory.PHYSICAL, AccessibilityType.TICKET_OFFICE),
            entry("wheelchair", AccessibilityCategory.PHYSICAL, AccessibilityType.WHEELCHAIR),
            entry("exit", AccessibilityCategory.PHYSICAL, AccessibilityType.EXIT),
            entry("elevator", AccessibilityCategory.PHYSICAL, AccessibilityType.ELEVATOR),
            entry("restroom", AccessibilityCategory.PHYSICAL, AccessibilityType.ACCESSIBLE_RESTROOM),
            entry("auditorium", AccessibilityCategory.PHYSICAL, AccessibilityType.AUDITORIUM),
            entry("room", AccessibilityCategory.PHYSICAL, AccessibilityType.ROOM),
            entry("handicapetc", AccessibilityCategory.PHYSICAL, AccessibilityType.ETC),
            entry("braileblock", AccessibilityCategory.VISUAL, AccessibilityType.BRAILLE_BLOCK),
            entry("helpdog", AccessibilityCategory.VISUAL, AccessibilityType.HELP_DOG),
            entry("guidehuman", AccessibilityCategory.VISUAL, AccessibilityType.GUIDE_HUMAN),
            entry("audioguide", AccessibilityCategory.VISUAL, AccessibilityType.AUDIO_GUIDE),
            entry("bigprint", AccessibilityCategory.VISUAL, AccessibilityType.BIG_PRINT),
            entry("brailepromotion", AccessibilityCategory.VISUAL, AccessibilityType.BRAILLE_PROMOTION),
            entry("guidesystem", AccessibilityCategory.VISUAL, AccessibilityType.GUIDE_SYSTEM),
            entry("blindhandicapetc", AccessibilityCategory.VISUAL, AccessibilityType.ETC),
            entry("signguide", AccessibilityCategory.HEARING, AccessibilityType.SIGN_GUIDE),
            entry("videoguide", AccessibilityCategory.HEARING, AccessibilityType.VIDEO_GUIDE),
            entry("hearingroom", AccessibilityCategory.HEARING, AccessibilityType.ROOM),
            entry("hearinghandicapetc", AccessibilityCategory.HEARING, AccessibilityType.ETC),
            entry("stroller", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.STROLLER),
            entry("lactationroom", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.LACTATION_ROOM),
            entry("babysparechair", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.BABY_SPARE_CHAIR),
            entry("infantsfamilyetc", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.ETC)
    );

    private static final Map<String, AccessibilityFieldMapping> KOR_INTRO_FIELD_MAP = Map.ofEntries(
            entry("parking", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("parkingculture", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("parkingleports", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("parkinglodging", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("parkingshopping", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("parkingfood", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("restroom", AccessibilityCategory.COMMON, AccessibilityType.RESTROOM),
            entry("chkbabycarriage", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.STROLLER),
            entry("chkbabycarriageculture", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.STROLLER),
            entry("chkbabycarriageleports", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.STROLLER),
            entry("chkbabycarriageshopping", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.STROLLER),
            entry("kidsfacility", AccessibilityCategory.INFANT_FAMILY, AccessibilityType.KIDS_FACILITY),
            entry("chkpet", AccessibilityCategory.COMMON, AccessibilityType.PET),
            entry("chkpetculture", AccessibilityCategory.COMMON, AccessibilityType.PET),
            entry("chkpetleports", AccessibilityCategory.COMMON, AccessibilityType.PET),
            entry("chkpetshopping", AccessibilityCategory.COMMON, AccessibilityType.PET)
    );

    /**
     * detailInfo2의 (infoname, infotext) 구조에서, infoname → (category, type) 매핑.
     *
     * <p>MVP에서는 주로 무장애(detailWithTour2)를 사용하지만, 이후 확장/보강용으로 남겨둔다.</p>
     */
    private static final Map<String, AccessibilityFieldMapping> KOR_INFO_NAME_MAP = Map.ofEntries(
            entry("장애인편의시설", AccessibilityCategory.PHYSICAL, AccessibilityType.ETC),
            entry("장애인 편의시설", AccessibilityCategory.PHYSICAL, AccessibilityType.ETC),
            entry("장애인화장실", AccessibilityCategory.COMMON, AccessibilityType.RESTROOM),
            entry("장애인 화장실", AccessibilityCategory.COMMON, AccessibilityType.RESTROOM),
            entry("이용가능시설", AccessibilityCategory.COMMON, AccessibilityType.ETC),
            entry("이용 가능 시설", AccessibilityCategory.COMMON, AccessibilityType.ETC),
            entry("주차가능", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("주차 가능", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("주차시설", AccessibilityCategory.COMMON, AccessibilityType.PARKING),
            entry("주차 시설", AccessibilityCategory.COMMON, AccessibilityType.PARKING)
    );

    public AccessibilityFieldMapping mapWithTourField(String sourceField) {
        return WITH_TOUR_FIELD_MAP.get(normalizeKey(sourceField));
    }

    public AccessibilityFieldMapping mapKorIntroField(String sourceField) {
        return KOR_INTRO_FIELD_MAP.get(normalizeKey(sourceField));
    }

    public AccessibilityFieldMapping mapKorInfoName(String infoName) {
        if (infoName == null) {
            return null;
        }
        return KOR_INFO_NAME_MAP.get(infoName.trim());
    }

    private static Map.Entry<String, AccessibilityFieldMapping> entry(
            String key,
            AccessibilityCategory category,
            AccessibilityType type
    ) {
        return Map.entry(key, new AccessibilityFieldMapping(category, type));
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
