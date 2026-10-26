package com.metergate.limiter.dto;

import java.time.LocalDateTime;

/**
 * Standard error response format — Rate Limiter Service ke liye.
 * Customer-plan-service wale ErrorResponse jaisa hi hai — same structure, bas package alag.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}