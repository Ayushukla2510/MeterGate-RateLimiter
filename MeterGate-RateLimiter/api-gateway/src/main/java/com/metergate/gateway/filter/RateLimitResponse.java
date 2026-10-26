package com.metergate.gateway.filter;

/**
 * Matches the JSON response returned by rate-limiter-service /limiter/check
 */
public record RateLimitResponse(
        boolean allowed,
        int limit,
        long remaining,
        long retryAfterSeconds,
        String rejectReason,
        long currentUsage
) {}