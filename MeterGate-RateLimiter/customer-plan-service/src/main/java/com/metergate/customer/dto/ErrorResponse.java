package com.metergate.customer.dto;

import java.time.LocalDateTime;

/**
 * Standard error response format for all API errors.
 * Har error same structure mein client ko milega — consistency!
 *
 * @param status    HTTP status code (e.g., 404, 400, 500)
 * @param error     HTTP status name (e.g., "NOT_FOUND", "BAD_REQUEST")
 * @param message   Human-readable error description
 * @param timestamp Jab error hua — exact time
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    /**
     * Factory method — quick error response banane ke liye.
     */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}