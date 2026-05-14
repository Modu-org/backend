package com.ssafy.modu.batch.tour.dto;

/**
 * detailWithTour2 호출 결과 요약.
 *
 * <p>item이 없는 케이스도 정상적으로 발생할 수 있어서, 단순 "저장 row 수"만으로는
 * 호출 완료 여부를 판단할 수 없다(hasItem 필요).</p>
 */
public record AccessibilityImportOutcome(
        boolean hasItem,
        int savedRows
) {
    public static AccessibilityImportOutcome empty() {
        return new AccessibilityImportOutcome(false, 0);
    }
}
