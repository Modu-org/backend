package com.ssafy.modu.domain.aicommand.dto.enums;

public enum VoiceCommandIntent {
    ATTRACTION_SEARCH,     // 관광지 검색
    ATTRACTION_DETAIL,     // 관광지 상세 조회/설명
    SCHEDULE_WORKFLOW,     // 관광지 선택 후 일정 찾기/생성/추가
    SCHEDULE_SCOPED,       // 특정 일정 안에서 순서 변경/자동 배치
    UNKNOWN                // 의도 불명확
}
