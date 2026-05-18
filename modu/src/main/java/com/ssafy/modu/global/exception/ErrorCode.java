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
    ATTRACTION_NOT_FOUND(HttpStatus.NOT_FOUND,904,"존재하지 않는 관광지입니다.");
    private final HttpStatus httpStatus;
    private final int status;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int status, String message) {
        this.httpStatus = httpStatus;
        this.status = status;
        this.message = message;
    }
}