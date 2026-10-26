package com.metergate.customer.dto;

import java.math.BigDecimal;

// Yeh response Gateway ko bheja jata hai jab Gateway key validate karta hai
public record ValidateKeyResponse(
        boolean valid,
        Long apiKeyId,
        Long userId,
        String planName,
        int rateLimitPerMinute,
        long monthlyQuota,
        String quotaPolicy,       // HARD_CAP or SOFT_CAP
        BigDecimal overagePricePerThousand,
        String status             // ACTIVE or REVOKED
) {}