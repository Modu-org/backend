package com.ssafy.modu.domain.arrival.dto.response;

import com.ssafy.modu.domain.arrival.entity.ArrivalLog;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.node.entity.Node;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArrivalLogDetailResponse {

    private Long arrivalLogId;

    private Long scheduleId;
    private Long nodeId;
    private Long attractionId;

    private String attractionName;
    private String address;

    private boolean arrived;
    private Integer distanceMeters;

    private Double requestLatitude;
    private Double requestLongitude;

    private Double attractionLatitude;
    private Double attractionLongitude;

    private LocalDateTime requestedAt;

    public static ArrivalLogDetailResponse of(
            ArrivalLog arrivalLog,
            Node node,
            Attraction attraction
    ) {
        return ArrivalLogDetailResponse.builder()
                .arrivalLogId(arrivalLog.getId())
                .scheduleId(arrivalLog.getScheduleId())
                .nodeId(arrivalLog.getNodeId())
                .attractionId(attraction.getId())
                .attractionName(attraction.getName())
                .address(attraction.getAddress())
                .arrived(arrivalLog.isArrived())
                .distanceMeters((int) Math.round(arrivalLog.getDistanceMeters()))
                .requestLatitude(arrivalLog.getRequestLatitude())
                .requestLongitude(arrivalLog.getRequestLongitude())
                .attractionLatitude(arrivalLog.getAttractionLatitude())
                .attractionLongitude(arrivalLog.getAttractionLongitude())
                .requestedAt(arrivalLog.getRequestedAt())
                .build();
    }
}