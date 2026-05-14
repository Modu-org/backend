package com.ssafy.modu.batch.tour.cursor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TourBatchCursorRepository extends JpaRepository<TourBatchCursor, Long> {

    Optional<TourBatchCursor> findByJobType(TourBatchCursorJobType jobType);
}