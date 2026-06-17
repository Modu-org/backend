package com.ssafy.modu.domain.aicommand.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class VoiceCommandRequest {

    @NotBlank(message = "사용자 명령은 비어 있을 수 없습니다.")
    private String text;

    private String screen;

    private Long attractionId;

    private Long scheduleId;

    private LocalDate date;

    private Boolean apply = true;

    private List<VisibleAttraction> visibleAttractions;

    public boolean isApply() {
        return apply == null || apply;
    }

    @Getter
    @NoArgsConstructor
    public static class VisibleAttraction {
        private Long attractionId;
        private String name;
    }
}
