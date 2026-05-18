package com.ssafy.modu.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(SuccessCode successCode, String message, T data) {
        return new ApiResponse<>(true, successCode.getCode(), message, data);
    }

    public static ApiResponse<Void> success(SuccessCode successCode, String message) {
        return new ApiResponse<>(true, successCode.getCode(), message, null);
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(true, code, message, data);
    }

    public static ApiResponse<Void> success(int code, String message) {
        return new ApiResponse<>(true, code, message, null);
    }
}