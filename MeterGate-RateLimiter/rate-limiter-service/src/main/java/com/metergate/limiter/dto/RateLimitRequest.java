package com.metergate.limiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Incoming request payload from API Gateway to check rate limit and monthly quota.
 */
public record RateLimitRequest(
        @NotNull(message = "apiKeyId is required")
        Long apiKeyId,

        @Min(value = 1, message = "rateLimitPerMinute must be at least 1")
        int rateLimitPerMinute,

        @Min(value = 1, message = "monthlyQuota must be at least 1")
        long monthlyQuota,

        @NotBlank(message = "quotaPolicy is required (HARD_CAP or SOFT_CAP)")
        String quotaPolicy
) {}