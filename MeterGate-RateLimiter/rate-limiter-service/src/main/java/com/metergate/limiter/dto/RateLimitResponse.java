package com.metergate.limiter.dto;

/**
 * Output response sent back to API Gateway, including rate limit and quota decisions.
 */
public record RateLimitResponse(
        boolean allowed,
        int limit,
        long remaining,
        long retryAfterSeconds,
        String rejectReason,    // null, "RATE_LIMIT_EXCEEDED", or "QUOTA_EXCEEDED"
        long currentUsage       // Total monthly usage so far
) {
    // 1. Helper: Request Allowed
    public static RateLimitResponse allow(int limit, long remaining, long currentUsage) {
        return new RateLimitResponse(true, limit, remaining, 0, null, currentUsage);
    }

    // 2. Helper: Reject due to Per-Minute Rate Limit (HTTP 429)
    public static RateLimitResponse rejectRateLimit(int limit, long retryAfterSeconds, long currentUsage) {
        return new RateLimitResponse(false, limit, 0, retryAfterSeconds, "RATE_LIMIT_EXCEEDED", currentUsage);
    }

    // 3. Helper: Reject due to Monthly Quota Hard Cap (HTTP 403)
    public static RateLimitResponse rejectQuota(int limit, long currentUsage) {
        return new RateLimitResponse(false, limit, 0, 0, "QUOTA_EXCEEDED", currentUsage);
    }
}