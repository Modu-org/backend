package com.ssafy.modu.global.exception;

import com.ssafy.modu.external.tourapi.TourApiTrafficExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TourApiTrafficExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTourApiTrafficExceeded(TourApiTrafficExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 429,
                "error", "TOO_MANY_REQUESTS",
                "message", e.getMessage()
        ));
    }
}

