package com.ssafy.modu.domain.arrival.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArrivalResponse {

    private boolean arrived;

    private String message;

    private String currentNodeName;

    private int distanceMeters;

    private NextDestinationResponse nextDestination;

    public static ArrivalResponse of(
            boolean arrived,
            String currentNodeName,
            double distanceMeters,
            NextDestinationResponse nextDestination
    ) {
        String message = arrived
                ? currentNodeName + " 도착이 확인되었습니다."
                : currentNodeName + " 기준 위치가 너무 멀어 도착 확인에 실패했습니다.";

        return ArrivalResponse.builder()
                .arrived(arrived)
                .message(message)
                .currentNodeName(currentNodeName)
                .distanceMeters((int) Math.round(distanceMeters))
                .nextDestination(nextDestination)
                .build();
    }
    public static ArrivalResponse alreadyArrived(
            String currentNodeName,
            double distanceMeters,
            NextDestinationResponse nextDestination
    ) {
        return ArrivalResponse.builder()
                .arrived(true)
                .message(currentNodeName + "은 이미 도착 확인된 장소입니다.")
                .currentNodeName(currentNodeName)
                .distanceMeters((int) Math.round(distanceMeters))
                .nextDestination(nextDestination)
                .build();
    }
}