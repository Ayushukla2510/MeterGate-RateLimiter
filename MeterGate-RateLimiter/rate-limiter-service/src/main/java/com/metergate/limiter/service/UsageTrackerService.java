package com.metergate.limiter.service;

import com.metergate.limiter.entity.UsageCounter;
import com.metergate.limiter.repository.UsageCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;

@Service
public class UsageTrackerService {

    private final UsageCounterRepository usageCounterRepository;

    // Constructor injection
    public UsageTrackerService(UsageCounterRepository usageCounterRepository) {
        this.usageCounterRepository = usageCounterRepository;
    }

    /**
     * Returns current billing period key, e.g. "2026-10"
     */
    public String getCurrentPeriod() {
        return YearMonth.now().toString();
    }

    /**
     * Checks if monthly quota is available, and if allowed, atomically increments the counter.
     * Returns a QuotaCheckResult indicating if request is allowed, along with current usage count.
     */
    @Transactional
    public QuotaCheckResult checkAndIncrement(Long apiKeyId, long monthlyQuota, String quotaPolicy) {
        String period = getCurrentPeriod();

        // 1. Ensure a usage counter record exists for this key + period
        UsageCounter counter = getOrCreateCounter(apiKeyId, period);
        long currentTotal = counter.getUsedCount() + counter.getOverageCount();

        // 2. Check if quota has been exceeded
        if (currentTotal >= monthlyQuota) {
            // HARD_CAP (e.g. FREE plan): Block further requests (HTTP 403)
            if ("HARD_CAP".equalsIgnoreCase(quotaPolicy)) {
                return new QuotaCheckResult(false, currentTotal);
            }

            // SOFT_CAP (e.g. PRO/ENTERPRISE plan): Allow request, but atomically track as overage
            usageCounterRepository.incrementOverageCount(apiKeyId, period);
            return new QuotaCheckResult(true, currentTotal + 1);
        }

        // 3. Within normal quota: Atomically increment regular used_count
        usageCounterRepository.incrementUsedCount(apiKeyId, period);
        return new QuotaCheckResult(true, currentTotal + 1);
    }

    /**
     * Fetch existing usage counter or atomically create a new 0-count row for a new period.
     */
    @Transactional
    public UsageCounter getOrCreateCounter(Long apiKeyId, String period) {
        Optional<UsageCounter> counterOpt = usageCounterRepository.findByApiKeyIdAndPeriod(apiKeyId, period);
        if (counterOpt.isPresent()) {
            return counterOpt.get();
        }

        // Initialize new counter for the current month
        try {
            UsageCounter newCounter = new UsageCounter(apiKeyId, period);
            return usageCounterRepository.save(newCounter);
        } catch (Exception e) {
            // In case of concurrent first requests, fallback to fetching the created row
            return usageCounterRepository.findByApiKeyIdAndPeriod(apiKeyId, period)
                    .orElseThrow(() -> new IllegalStateException("Failed to initialize usage counter", e));
        }
    }

    /**
     * Record to hold quota check result internally
     */
    public record QuotaCheckResult(boolean allowed, long currentUsage) {}
}