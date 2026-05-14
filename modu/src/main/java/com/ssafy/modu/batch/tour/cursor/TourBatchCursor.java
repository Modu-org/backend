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

    protected TourBatchCursor(TourBatchCursorJobType jobType) {
        this.jobType = jobType;
        this.nextPage = 1;
        this.completed = false;
    }

    public static TourBatchCursor create(TourBatchCursorJobType jobType) {
        return new TourBatchCursor(jobType);
    }

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

    public void reset() {
        this.nextPage = 1;
        this.totalCount = null;
        this.lastRequestedPage = null;
        this.completed = false;
        this.lastRunAt = LocalDateTime.now();
    }
}