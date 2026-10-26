package com.metergate.limiter.controller;

import com.metergate.limiter.dto.RateLimitRequest;
import com.metergate.limiter.dto.RateLimitResponse;
import com.metergate.limiter.service.BucketManager;
import com.metergate.limiter.service.RateLimiterMetrics;
import com.metergate.limiter.service.UsageTrackerService;
import com.metergate.limiter.service.UsageTrackerService.QuotaCheckResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/limiter")
public class RateLimiterController {

    private final BucketManager bucketManager;
    private final UsageTrackerService usageTrackerService;
    private final RateLimiterMetrics metrics;

    // Constructor injection for BucketManager, UsageTrackerService, and Micrometer Metrics
    public RateLimiterController(BucketManager bucketManager,
                                 UsageTrackerService usageTrackerService,
                                 RateLimiterMetrics metrics) {
        this.bucketManager = bucketManager;
        this.usageTrackerService = usageTrackerService;
        this.metrics = metrics;
    }

    /**
     * Endpoint called by API Gateway to check both:
     * 1. Per-minute rate limit (Bucket4j in-memory)
     * 2. Monthly quota allowance (MySQL atomic)
     *
     * POST /limiter/check
     */
    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@Valid @RequestBody RateLimitRequest request) {

        // STEP 1: Fast in-memory Token Bucket check (Nanoseconds)
        RateLimitResponse bucketResponse = bucketManager.tryConsume(
                request.apiKeyId(),
                request.rateLimitPerMinute()
        );

        // If per-minute rate limit exceeded -> Record metric & 429 Too Many Requests
        if (!bucketResponse.allowed()) {
            metrics.incrementRateLimitRejected();

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Limit", String.valueOf(bucketResponse.limit()))
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", String.valueOf(bucketResponse.retryAfterSeconds()))
                    .body(RateLimitResponse.rejectRateLimit(
                            bucketResponse.limit(),
                            bucketResponse.retryAfterSeconds(),
                            0
                    ));
        }

        // STEP 2: Monthly Quota check in Database (Milliseconds)
        QuotaCheckResult quotaResult = usageTrackerService.checkAndIncrement(
                request.apiKeyId(),
                request.monthlyQuota(),
                request.quotaPolicy()
        );

        // If monthly quota exceeded on HARD_CAP (e.g. FREE plan) -> Record metric & 403 Forbidden
        if (!quotaResult.allowed()) {
            metrics.incrementQuotaRejected();

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-RateLimit-Limit", String.valueOf(request.rateLimitPerMinute()))
                    .header("X-RateLimit-Remaining", "0")
                    .body(RateLimitResponse.rejectQuota(
                            request.rateLimitPerMinute(),
                            quotaResult.currentUsage()
                    ));
        }

        // STEP 3: Both checks passed -> Record allowed metric & return 200 OK
        metrics.incrementAllowed();

        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(bucketResponse.limit()))
                .header("X-RateLimit-Remaining", String.valueOf(bucketResponse.remaining()))
                .header("X-Monthly-Usage", String.valueOf(quotaResult.currentUsage()))
                .body(RateLimitResponse.allow(
                        bucketResponse.limit(),
                        bucketResponse.remaining(),
                        quotaResult.currentUsage()
                ));
    }
}