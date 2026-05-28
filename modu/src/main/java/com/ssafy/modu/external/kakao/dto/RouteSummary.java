package com.ssafy.modu.external.kakao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RouteSummary {

    private int distanceMeters;
    private int durationSeconds;

    public int getDurationMinutes() {
        return (int) Math.ceil(durationSeconds / 60.0);
    }
}