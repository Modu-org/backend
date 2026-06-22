package com.ssafy.modu.batch.tour.cursor;

import com.ssafy.modu.batch.tour.dto.TourImportResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "tour_batch_cursor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tour_batch_cursor_job_type", columnNames = "job_type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourBatchCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private TourBatchCursorJobType jobType;

    @Column(name = "next_page", nullable = false)
    private int nextPage;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "last_requested_page")
    private Integer lastRequestedPage;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /**
     * 변경분 동기화에서 사용 중인 modifiedTime 기준값.
     *
     * 변경분 처리가 completed=false로 끝난 경우,
     * 다음 실행 때 새로운 modifiedTime을 만들지 않고
     * 이 값을 기준으로 nextPage부터 이어서 처리한다.
     */
    @Column(name = "running_modified_time", length = 14)
    private String runningModifiedTime;

    protected TourBatchCursor(TourBatchCursorJobType jobType) {
        this.jobType = jobType;
        this.nextPage = 1;
        this.completed = false;
    }

    public static TourBatchCursor create(TourBatchCursorJobType jobType) {
        return new TourBatchCursor(jobType);
    }

    /**
     * 초기 전체 적재용 cursor 갱신.
     */
    public void updateAfterRun(TourImportResult result) {
        this.totalCount = result.totalCount();
        this.lastRequestedPage = result.lastRequestedPage();
        this.completed = result.completed();
        this.lastRunAt = LocalDateTime.now();

        if (result.completed()) {
            this.nextPage = result.lastRequestedPage();
        } else if (result.nextPage() != null) {
            this.nextPage = result.nextPage();
        }
    }

    /**
     * 변경분 동기화 시작.
     *
     * 새로운 modifiedTime 기준으로 1페이지부터 처리한다.
     */
    public void startModifiedSync(String modifiedTime) {
        this.runningModifiedTime = modifiedTime;
        this.nextPage = 1;
        this.totalCount = null;
        this.lastRequestedPage = null;
        this.completed = false;
        this.lastRunAt = LocalDateTime.now();
    }

    /**
     * 변경분 동기화 결과 반영.
     *
     * completed=false:
     * - runningModifiedTime 유지
     * - nextPage 저장
     *
     * completed=true:
     * - runningModifiedTime 제거
     * - 다음 실행 때 새로운 modifiedTime으로 시작 가능
     */
    public void updateAfterModifiedSync(TourImportResult result) {
        this.totalCount = result.totalCount();
        this.lastRequestedPage = result.lastRequestedPage();
        this.completed = result.completed();
        this.lastRunAt = LocalDateTime.now();

        if (result.completed()) {
            this.nextPage = 1;
            this.runningModifiedTime = null;
        } else if (result.nextPage() != null) {
            this.nextPage = result.nextPage();
        }
    }

    public void reset() {
        this.nextPage = 1;
        this.totalCount = null;
        this.lastRequestedPage = null;
        this.completed = false;
        this.runningModifiedTime = null;
        this.lastRunAt = LocalDateTime.now();
    }
}