package com.ssafy.modu.domain.arrival.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "arrival_log")
public class ArrivalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "arrival_log_id")
    private Long id;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Long nodeId;

    @Column(nullable = false)
    private Long travelerId;

    @Column(nullable = false)
    private boolean arrived;

    @Column(nullable = false)
    private double requestLatitude;

    @Column(nullable = false)
    private double requestLongitude;

    @Column(nullable = false)
    private double attractionLatitude;

    @Column(nullable = false)
    private double attractionLongitude;

    @Column(nullable = false)
    private double distanceMeters;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    public static ArrivalLog create(
            Long scheduleId,
            Long nodeId,
            Long travelerId,
            boolean arrived,
            double requestLatitude,
            double requestLongitude,
            double attractionLatitude,
            double attractionLongitude,
            double distanceMeters
    ) {
        ArrivalLog arrivalLog = new ArrivalLog();
        arrivalLog.scheduleId = scheduleId;
        arrivalLog.nodeId = nodeId;
        arrivalLog.travelerId = travelerId;
        arrivalLog.arrived = arrived;
        arrivalLog.requestLatitude = requestLatitude;
        arrivalLog.requestLongitude = requestLongitude;
        arrivalLog.attractionLatitude = attractionLatitude;
        arrivalLog.attractionLongitude = attractionLongitude;
        arrivalLog.distanceMeters = distanceMeters;
        arrivalLog.requestedAt = LocalDateTime.now();
        return arrivalLog;
    }
}