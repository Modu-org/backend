package com.ssafy.modu.global.util.parser;

import com.ssafy.modu.domain.accessibility.entity.enums.AccessibilityStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessibilityStatusParser {

    /**
     * rawValue 문자열을 status로 파싱한다.
     *
     * <p>우선순위(계획서 기준):
     * UNKNOWN(빈값) → UNAVAILABLE(부정) → PARTIAL(부분/조건부) → NEED_CHECK(확인 필요) → AVAILABLE(긍정) → NEED_CHECK</p>
     */
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "없음",
            "불가",
            "미설치",
            "미제공",
            "제공 안함",
            "제공안함",
            "이용불가",
            "해당없음",
            "해당 없음"
    );

    private static final List<String> PARTIAL_KEYWORDS = List.of(
            "부분",
            "일부",
            "일부구역",
            "일부 구역",
            "일부공간",
            "일부 공간",
            "보조 필요",
            "직원 지원",
            "제한",
            "조건"
    );

    private static final List<String> NEED_CHECK_KEYWORDS = List.of(
            "문의",
            "확인",
            "사전",
            "변동",
            "현장",
            "유동",
            "미정"
    );

    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "있음",
            "가능",
            "대여",
            "설치",
            "운영",
            "제공",
            "구비",
            "동반 가능"
    );

    public AccessibilityStatus parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AccessibilityStatus.UNKNOWN;
        }

        String normalized = normalize(rawValue);

        if (containsAny(normalized, NEGATIVE_KEYWORDS)) {
            return AccessibilityStatus.UNAVAILABLE;
        }
        if (containsAny(normalized, PARTIAL_KEYWORDS)) {
            return AccessibilityStatus.PARTIAL;
        }
        if (containsAny(normalized, NEED_CHECK_KEYWORDS)) {
            return AccessibilityStatus.NEED_CHECK;
        }
        if (containsAny(normalized, POSITIVE_KEYWORDS)) {
            return AccessibilityStatus.AVAILABLE;
        }
        return AccessibilityStatus.NEED_CHECK;
    }

    private String normalize(String value) {
        return value
                .replace("<br>", " ")
                .replace("<br/>", " ")
                .replace("<br />", " ")
                .replace("&lt;br&gt;", " ")
                .replace("_", " ")
                .trim();
    }

    private boolean containsAny(String value, List<String> keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
