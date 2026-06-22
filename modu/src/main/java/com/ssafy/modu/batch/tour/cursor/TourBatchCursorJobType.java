package com.ssafy.modu.batch.tour.cursor;

public enum TourBatchCursorJobType {
    // 초기 전체 적재용
    GENERAL_LIST,
    ACCESSIBLE_LIST,

    // 변경분 동기화용
    GENERAL_MODIFIED_SYNC,
    ACCESSIBLE_MODIFIED_SYNC
}