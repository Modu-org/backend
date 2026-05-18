package com.ssafy.modu.domain.attraction.dto.condition;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AttractionSearchCondition {

    private String regionCode;
    private String sigunguCode;
    private String keyword;

    private List<String> contentTypeIds;

    private List<AccessibilityCategory> categories;
}