package com.ssafy.modu.domain.aicommand.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AiScheduleCommandRequest {

    /**
     * 특정 날짜 안에서 노드 순서를 바꾸는 명령에서 사용한다.
     * 일정 생성/관광지 추가/전체 자동 배치처럼 날짜가 필요 없는 명령에서는 null일 수 있다.
     */
    private LocalDate date;

    @NotBlank(message = "사용자 명령은 비어 있을 수 없습니다.")
    private String text;

    /**
     * 관광지 목록/상세 화면에서 프론트가 확정해서 전달한 단일 관광지 ID.
     * AI가 임의로 관광지 ID를 만들지 못하도록 Tool Handler에서 이 값 기준으로 검증한다.
     */
    private Long attractionId;

    /**
     * true이면 실제 DB에 반영한다.
     * false이면 추후 미리보기 기능에서 사용한다.
     */
    private Boolean apply = true;

    public boolean isApply() {
        return apply == null || apply;
    }
}
