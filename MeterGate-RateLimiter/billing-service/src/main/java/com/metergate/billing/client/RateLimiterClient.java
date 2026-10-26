package com.metergate.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "rate-limiter-service")
public interface RateLimiterClient {

    /**
     * Calls rate-limiter-service to fetch usage stats for a given apiKeyId and period.
     * GET http://rate-limiter-service/usage/{apiKeyId}?period=2026-10
     */
    @GetMapping("/usage/{apiKeyId}")
    UsageDto getUsage(
            @PathVariable("apiKeyId") Long apiKeyId,
            @RequestParam("period") String period
    );
}