package com.ssafy.modu.domain.aicommand.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiScheduleCommandRequest {

    private LocalDate date;

    @NotBlank(message = "사용자 명령은 비어 있을 수 없습니다.")
    private String text;

    private Long attractionId;

    private Boolean apply = true;

    public boolean isApply() {
        return apply == null || apply;
    }

    public static AiScheduleCommandRequest of(
            LocalDate date,
            String text,
            Long attractionId,
            Boolean apply
    ) {
        return new AiScheduleCommandRequest(date, text, attractionId, apply);
    }
}
