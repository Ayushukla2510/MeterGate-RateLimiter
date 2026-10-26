package com.metergate.gateway.filter;

/**
 * Payload sent by API Gateway to rate-limiter-service /limiter/check
 */
public record RateLimitRequest(
        Long apiKeyId,
        int rateLimitPerMinute,
        long monthlyQuota,
        String quotaPolicy
) {}