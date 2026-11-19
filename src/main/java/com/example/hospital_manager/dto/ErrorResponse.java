package com.example.hospital_manager.dto;

import java.time.LocalDateTime;

/**
 * Uniform JSON error body returned by the GlobalExceptionHandler.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}
