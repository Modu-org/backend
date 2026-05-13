package com.ssafy.modu.domain.accessibility.entity.converter;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessibilityStatusConverter
        implements AttributeConverter<AccessibilityStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AccessibilityStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AccessibilityStatus convertToEntityAttribute(Integer dbData) {
        return AccessibilityStatus.fromCode(dbData);
    }
}