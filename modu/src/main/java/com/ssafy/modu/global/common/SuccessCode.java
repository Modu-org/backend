package com.ssafy.modu.global.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode {

    OK(HttpStatus.OK, 700),
    CREATED(HttpStatus.CREATED, 701);

    private final HttpStatus httpStatus;
    private final int code;

    SuccessCode(HttpStatus httpStatus, int code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}