package com.metergate.gateway.filter;

import java.math.BigDecimal;

/**
 * Matches the JSON response sent by customer-plan-service /keys/validate
 */
public record ValidateKeyResponse(
        boolean valid,
        Long apiKeyId,
        Long userId,
        String planName,
        int rateLimitPerMinute,
        long monthlyQuota,
        String quotaPolicy,
        BigDecimal overagePricePerThousand,
        String status
) {}