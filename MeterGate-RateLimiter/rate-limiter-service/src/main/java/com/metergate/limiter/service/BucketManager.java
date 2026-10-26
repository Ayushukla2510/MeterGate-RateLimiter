package com.metergate.limiter.service;

import com.metergate.limiter.dto.RateLimitResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class BucketManager {

    // Thread-safe map to store one token bucket per apiKeyId
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks rate limit for a given apiKeyId.
     * Consumes 1 token if available, or calculates wait time if exhausted.
     */
    public RateLimitResponse tryConsume(Long apiKeyId, int rateLimitPerMinute) {
        // 1. Get existing bucket or create a new one atomically if not present
        Bucket bucket = buckets.computeIfAbsent(apiKeyId, id -> createNewBucket(rateLimitPerMinute));

        // 2. Try to consume 1 token atomically (Thread-safe via Bucket4j CAS)
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // 3. If token successfully consumed -> ALLOW (pass 0L for currentUsage, Controller will enrich it)
        if (probe.isConsumed()) {
            return RateLimitResponse.allow(rateLimitPerMinute, probe.getRemainingTokens(), 0L);
        }

        // 4. If tokens exhausted -> REJECT (Calculate retry-after seconds)
        long nanosToWait = probe.getNanosToWaitForRefill();
        long secondsToWait = TimeUnit.NANOSECONDS.toSeconds(nanosToWait);

        // At least 1 second wait time so client doesn't retry instantly
        long retryAfterSeconds = Math.max(1, secondsToWait);

        return RateLimitResponse.rejectRateLimit(rateLimitPerMinute, retryAfterSeconds, 0L);
    }
    /**
     * Helper to configure and build a Token Bucket using Bucket4j.
     */
    private Bucket createNewBucket(int rateLimitPerMinute) {
        // Bandwidth defines capacity and greedy refill rate over 1 minute
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(rateLimitPerMinute)
                .refillGreedy(rateLimitPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Helper to clear buckets (useful for tests or evicting revoked keys).
     */
    public void evictBucket(Long apiKeyId) {
        buckets.remove(apiKeyId);
    }
}