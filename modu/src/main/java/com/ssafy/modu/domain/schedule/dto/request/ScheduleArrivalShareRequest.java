package com.ssafy.modu.domain.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleArrivalShareRequest {

    @NotNull(message = "도착 알림 설정 값은 필수입니다.")
    private Boolean enabled;
}