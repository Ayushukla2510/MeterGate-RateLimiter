package com.metergate.limiter.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterMetrics {

    private final Counter allowedRequestsCounter;
    private final Counter rateLimitRejectedCounter;
    private final Counter quotaRejectedCounter;

    // MeterRegistry is Spring Boot's metrics registry (Micrometer)
    public RateLimiterMetrics(MeterRegistry registry) {
        this.allowedRequestsCounter = Counter.builder("metergate.requests.allowed")
                .description("Total number of successfully allowed API requests")
                .register(registry);

        this.rateLimitRejectedCounter = Counter.builder("metergate.requests.rejected")
                .tag("reason", "rate_limit_429")
                .description("Total number of requests blocked by per-minute rate limiting")
                .register(registry);

        this.quotaRejectedCounter = Counter.builder("metergate.requests.rejected")
                .tag("reason", "quota_exceeded_403")
                .description("Total number of requests blocked by monthly quota hard cap")
                .register(registry);
    }

    public void incrementAllowed() {
        allowedRequestsCounter.increment();
    }

    public void incrementRateLimitRejected() {
        rateLimitRejectedCounter.increment();
    }

    public void incrementQuotaRejected() {
        quotaRejectedCounter.increment();
    }
}