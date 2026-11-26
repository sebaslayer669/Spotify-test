package com.quipux.prueba.shared.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiError(
        String timestamp,
        int status,
        String error,
        String message
) {
    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message == null ? status.getReasonPhrase() : message
        );
    }
}
