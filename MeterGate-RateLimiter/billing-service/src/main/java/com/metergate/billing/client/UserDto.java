package com.metergate.billing.client;

/**
 * Holds User and Plan details fetched from customer-plan-service via Feign.
 */
public record UserDto(
        Long id,
        String name,
        String email,
        PlanDto plan
) {}