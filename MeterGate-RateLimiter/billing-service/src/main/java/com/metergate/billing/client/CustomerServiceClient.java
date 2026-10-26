package com.metergate.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-plan-service")
public interface CustomerServiceClient {

    /**
     * Calls customer-plan-service to fetch User and Plan details.
     * GET http://customer-plan-service/api/v1/customers/{userId}
     */
    @GetMapping("/api/v1/customers/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);
}