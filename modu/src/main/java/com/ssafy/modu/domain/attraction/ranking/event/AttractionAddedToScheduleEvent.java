package com.ssafy.modu.domain.attraction.ranking.event;

public record AttractionAddedToScheduleEvent(
        Long attractionId,
        String regionCode
) {
}
