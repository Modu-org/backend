package com.ssafy.modu.domain.accessibility.entity.converter;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessibilityTypeConverter
        implements AttributeConverter<AccessibilityType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AccessibilityType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AccessibilityType convertToEntityAttribute(Integer dbData) {
        return AccessibilityType.fromCode(dbData);
    }
}