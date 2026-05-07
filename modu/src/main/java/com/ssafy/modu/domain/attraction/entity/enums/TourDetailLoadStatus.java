package com.ssafy.modu.domain.attraction.entity.enums;

/**
 * 외부 Tour API 상세 호출 상태.
 *
 * 배치가 "아이템이 없어서 저장할 것이 없는 케이스"를 만났을 때도 NO_DATA로 마킹하여
 * 무한 재호출(무한 재시도)되지 않도록 하기 위한 상태값
 */
public enum TourDetailLoadStatus {
    NOT_STARTED,
    SUCCESS,
    NO_DATA
}
