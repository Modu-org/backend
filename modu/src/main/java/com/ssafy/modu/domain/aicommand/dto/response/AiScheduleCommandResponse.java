package com.ssafy.modu.domain.aicommand.dto.response;

import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiScheduleCommandResponse {

    private String message;
    private ScheduleDetailResponse schedule;
    private List<String> executedTools;
}
