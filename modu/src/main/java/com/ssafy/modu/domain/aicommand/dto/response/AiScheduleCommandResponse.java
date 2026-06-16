package com.ssafy.modu.domain.aicommand.dto.response;

import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiScheduleCommandResponse {

    private String assistantMessage;
    private ScheduleDetailResponse schedule;

    public static AiScheduleCommandResponse of(
            String assistantMessage,
            ScheduleDetailResponse schedule
    ) {
        return new AiScheduleCommandResponse(assistantMessage, schedule);
    }
}