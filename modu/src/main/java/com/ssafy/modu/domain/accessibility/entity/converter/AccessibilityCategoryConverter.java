package com.ssafy.modu.domain.accessibility.entity.converter;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessibilityCategoryConverter
        implements AttributeConverter<AccessibilityCategory, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AccessibilityCategory attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AccessibilityCategory convertToEntityAttribute(Integer dbData) {
        return AccessibilityCategory.fromCode(dbData);
    }
}