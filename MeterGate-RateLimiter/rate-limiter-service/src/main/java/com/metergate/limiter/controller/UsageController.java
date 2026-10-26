package com.metergate.limiter.controller;

import com.metergate.limiter.entity.UsageCounter;
import com.metergate.limiter.repository.UsageCounterRepository;
import com.metergate.limiter.service.UsageTrackerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usage")
public class UsageController {

    private final UsageCounterRepository usageCounterRepository;
    private final UsageTrackerService usageTrackerService;

    // Constructor injection
    public UsageController(UsageCounterRepository usageCounterRepository,
                           UsageTrackerService usageTrackerService) {
        this.usageCounterRepository = usageCounterRepository;
        this.usageTrackerService = usageTrackerService;
    }

    /**
     * Inspect usage for an API Key in a given billing period.
     * Example: GET /usage/1?period=2026-10
     * If period is omitted, defaults to the current month.
     */
    @GetMapping("/{apiKeyId}")
    public ResponseEntity<Map<String, Object>> getUsage(
            @PathVariable("apiKeyId") Long apiKeyId,
            @RequestParam(name = "period", required = false) String period) {

        // If period query param not provided, use current period (e.g. "2026-10")
        String targetPeriod = (period != null && !period.isBlank())
                ? period
                : usageTrackerService.getCurrentPeriod();

        Optional<UsageCounter> counterOpt = usageCounterRepository.findByApiKeyIdAndPeriod(apiKeyId, targetPeriod);

        long usedCount = counterOpt.map(UsageCounter::getUsedCount).orElse(0L);
        long overageCount = counterOpt.map(UsageCounter::getOverageCount).orElse(0L);

        Map<String, Object> response = Map.of(
                "apiKeyId", apiKeyId,
                "period", targetPeriod,
                "usedCount", usedCount,
                "overageCount", overageCount,
                "totalUsage", (usedCount + overageCount)
        );

        return ResponseEntity.ok(response);
    }
}