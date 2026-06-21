package com.ssafy.modu.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_SIGNUP_REQUEST(HttpStatus.BAD_REQUEST, 900, "회원가입 입력값이 올바르지 않습니다."),
    INVALID_LOGIN_REQUEST(HttpStatus.BAD_REQUEST, 900, "아이디와 비밀번호를 입력해주세요."),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, 900, "아이디를 입력해주세요."),
    INVALID_USERNAME_FORMAT(HttpStatus.BAD_REQUEST, 900, "아이디 형식이 올바르지 않습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 901, "아이디 또는 비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, 901, "Refresh Token이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 901, "잘못된 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 901, "만료 Refresh Token입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, 901, "Refresh Token이 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 901, "인증이 필요합니다."),

    DUPLICATE_USER_NAME(HttpStatus.CONFLICT, 909, "이미 사용 중인 아이디입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "사용자를 찾을 수 없습니다."),

    TOUR_API_TRAFFIC_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 429, "Tour API 요청 한도를 초과했습니다."),
    ATTRACTION_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "존재하지 않는 관광지입니다."),

    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "존재하지 않는 여행 스케줄입니다."),
    NODE_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "존재하지 않는 노드입니다."),
    INVALID_TRIP_DATE(HttpStatus.BAD_REQUEST, 904, "여행 시작일은 종료일보다 늦을 수 없습니다."),
    INVALID_BUDGET(HttpStatus.BAD_REQUEST, 904, "예산은 0원 이상이어야 합니다."),
    INVALID_PEOPLE_COUNT(HttpStatus.BAD_REQUEST, 904, "인원 수는 1명 이상이어야합니다."),
    VISIT_ORDER_REQUIRED(HttpStatus.BAD_REQUEST,904,"방문 순서가 정해져있어야합니다."),
    INVALID_UNASSIGNED_NODE_ORDER(HttpStatus.BAD_REQUEST,904,"방문 순서가 정해져있을 수 없습니다."),
    INVALID_NODE_VISIT_DATE(HttpStatus.BAD_REQUEST, 904, "노드 방문일자는 여행 기간 안에 있어야 합니다."),
    INVALID_NODE_VISIT_ORDER(HttpStatus.BAD_REQUEST, 904, "노드 방문 순서는 1 이상이어야 합니다."),
    DUPLICATE_NODE_IN_REQUEST(HttpStatus.BAD_REQUEST, 904, "중복된 노드가 요청에 포함되어 있습니다."),
    NODE_ARRANGEMENT_EMPTY(HttpStatus.BAD_REQUEST, 909, "노드 배치 정보가 비어 있습니다."),
    DUPLICATE_VISIT_ORDER(HttpStatus.BAD_REQUEST, 909, "방문 순서가 겹칩니다."),
    INVALID_VISIT_ORDER_SEQUENCE(HttpStatus.BAD_REQUEST, 909, "방문 순서는 1부터 연속되어야 합니다."),
    AI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "AI API 호출에 실패했습니다."),
    INVALID_ROUTE_RECOMMENDATION_REQUEST(HttpStatus.BAD_REQUEST, 940, "경로 추천 요청이 올바르지 않습니다."),
    INVALID_AI_RECOMMENDATION(HttpStatus.INTERNAL_SERVER_ERROR, 941, "AI 경로 추천 결과가 올바르지 않습니다."),
    AI_ROUTE_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 942, "AI 경로 추천 처리에 실패했습니다."),
    VOICE_SEARCH_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "음성 검색 결과 파싱에 실패했습니다."),
    INVALID_VOICE_SEARCH_TYPE(HttpStatus.BAD_REQUEST, 900, "지원하지 않는 음성 검색 유형입니다."),
    KAKAO_DIRECTIONS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,500 , "KAKAO MoBILITY API 호출에 실패했습니다."),
    INVALID_EDGE_REQUEST(HttpStatus.BAD_REQUEST, 904,"유효하지 않은 간선 요청입니다."),
    INVALID_ATTRACTION_LOCATION(HttpStatus.BAD_REQUEST, 904,"관광지 좌표 정보가 올바르지 않습니다."),
    INVALID_REVIEW_REQUEST(HttpStatus.BAD_REQUEST, 904, "리뷰 입력값이 올바르지 않습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, 920, "이미 해당 관광지에 리뷰를 작성했습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "존재하지 않는 리뷰입니다."),
    REVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, 901, "리뷰에 접근할 권한이 없습니다."),

    UNSUPPORTED_AI_TOOL(HttpStatus.BAD_REQUEST, 950, "지원하지 않는 AI 도구입니다."),
    INVALID_AI_TOOL_ARGUMENTS(HttpStatus.BAD_REQUEST, 951, "AI 도구 호출 인자가 올바르지 않습니다."),
    UNSUPPORTED_AI_TOOL_OPERATION(HttpStatus.BAD_REQUEST, 952, "지원하지 않는 AI 도구 작업입니다."),
    AI_TOOL_CALL_LIMIT_EXCEEDED(HttpStatus.INTERNAL_SERVER_ERROR, 953, "AI 도구 호출 횟수를 초과했습니다."),
    INVALID_AI_SCHEDULE_RESPONSE(HttpStatus.BAD_REQUEST, 954, "AI 일정 처리 결과를 해석할 수 없습니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "FCM 알림 전송에 실패했습니다."),
    INVALID_ARRIVAL_NODE(HttpStatus.BAD_REQUEST, 900, "도착 확인을 할 수 없는 노드입니다."),
    INVALID_SCHEDULE_SHARE_REQUEST(HttpStatus.BAD_REQUEST, 900, "스케줄 공유 요청이 올바르지 않습니다."),
    SCHEDULE_SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "스케줄 공유 정보를 찾을 수 없습니다."),
    INVALID_CAREGIVER_RELATION_REQUEST(HttpStatus.BAD_REQUEST, 900, "보호자 관계 요청이 올바르지 않습니다."),
    CAREGIVER_RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "보호자 관계를 찾을 수 없습니다.");
    private final HttpStatus httpStatus;
    private final int status;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int status, String message) {
        this.httpStatus = httpStatus;
        this.status = status;
        this.message = message;
    }
}