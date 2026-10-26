package com.metergate.billing.client;

/**
 * Holds usage stats fetched from rate-limiter-service via Feign.
 */
public record UsageDto(
        Long apiKeyId,
        String period,
        long usedCount,
        long overageCount,
        long totalUsage
) {}