package com.metergate.billing.client;

import java.math.BigDecimal;

/**
 * Holds Plan details fetched from customer-plan-service via Feign.
 */
public record PlanDto(
        Long id,
        String name,
        BigDecimal basePrice,
        int rateLimitPerMinute,
        long monthlyQuota,
        String quotaPolicy,
        BigDecimal overagePricePerThousand
) {}