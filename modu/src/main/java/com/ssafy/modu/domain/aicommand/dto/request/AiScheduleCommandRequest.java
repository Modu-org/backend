package com.ssafy.modu.domain.aicommand.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AiScheduleCommandRequest {

    @NotNull(message = "일정 변경 대상 날짜는 필수입니다.")
    private LocalDate date;

    @NotBlank(message = "사용자 명령은 비어 있을 수 없습니다.")
    private String text;

    /**
     * true이면 실제 DB에 반영한다.
     * false이면 추후 미리보기 기능에서 사용한다.
     * 1차 구현에서는 rearrange_nodes Tool이 true 기준으로 동작한다.
     */
    private boolean apply = true;
}
