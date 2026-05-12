package com.ssafy.modu.external.tourapi;

/**
 * Thrown when the external Tour API rejects requests due to traffic/quota limits (e.g., HTTP 429).
 *
 * The intent is to stop batch loops immediately to avoid hammering the API until the next window.
 */
public class TourApiTrafficExceededException extends RuntimeException {

    private final Integer httpStatus;

    public TourApiTrafficExceededException(String message) {
        this(message, null, null);
    }

    public TourApiTrafficExceededException(String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}

