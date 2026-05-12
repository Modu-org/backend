package com.ssafy.modu.domain.user.dto;

public record CheckIdResponse(
        String userName,
        boolean available
) {
}