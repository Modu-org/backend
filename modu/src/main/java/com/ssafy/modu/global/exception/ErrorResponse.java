package com.ssafy.modu.global.exception;

public record ErrorResponse(
        boolean success,
        String message,
        String errorCode
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                false,
                errorCode.getMessage(),
                errorCode.name()
        );
    }
}