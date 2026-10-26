package com.metergate.customer.dto;

import java.time.LocalDateTime;

// Yeh response customer ko sirf EK BAAR dikhaya jata hai jab key generate hoti hai
// Iske baad rawKey kabhi kisi response mein wapas nahi aayegi!
public record ApiKeyResponse(
        Long keyId,
        Long userId,
        String rawKey,        // e.g. "sk_live_abc123..." (Showed once!)
        String keyPrefix,     // e.g. "sk_live_abc..."
        String status,        // ACTIVE
        LocalDateTime createdAt
) {}