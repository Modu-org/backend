package com.ssafy.modu.domain.aicommand.dto.tool;

import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ToolExecutionContext {

    private Long userId;

    /**
     * /api/schedules/{scheduleId}/ai-command에서는 값이 존재한다.
     * /api/ai/schedule-workflow처럼 전역 워크플로우 명령에서는 null일 수 있다.
     */
    private Long scheduleId;

    private LocalDate date;

    private boolean apply;

    /**
     * 관광지 목록/상세 화면에서 프론트가 확정해서 넘긴 단일 관광지 ID.
     * 일정 순서 변경처럼 관광지 선택과 무관한 명령에서는 null일 수 있다.
     */
    private Long attractionId;

    /**
     * 현재 AI 명령의 실행 범위.
     *
     * SCHEDULE_SCOPED:
     * - 특정 일정 화면 내부 명령
     * - scheduleId가 path variable로 확정된 상태
     *
     * SCHEDULE_WORKFLOW:
     * - 홈/관광지 목록/관광지 상세 등 전역 워크플로우 명령
     * - 일정 찾기 또는 새 일정 생성이 필요할 수 있음
     */
    private AiCommandScope scope;
}