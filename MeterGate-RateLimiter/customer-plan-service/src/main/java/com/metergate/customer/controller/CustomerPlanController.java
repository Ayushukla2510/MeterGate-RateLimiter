package com.metergate.customer.controller;

import com.metergate.customer.dto.ApiKeyResponse;
import com.metergate.customer.dto.CreateUserRequest;
import com.metergate.customer.dto.ValidateKeyResponse;
import com.metergate.customer.entity.User;
import com.metergate.customer.service.CustomerPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerPlanController {

    private final CustomerPlanService customerPlanService;

    // Constructor injection
    public CustomerPlanController(CustomerPlanService customerPlanService) {
        this.customerPlanService = customerPlanService;
    }

    /**
     * Get user details by ID (Called by billing-service via OpenFeign)
     * GET /api/v1/customers/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable("userId") Long userId) {
        User user = customerPlanService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * 1. Register a new user
     * POST /api/v1/customers
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User createdUser = customerPlanService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * 2. Generate a new API Key for a user
     * POST /api/v1/customers/{userId}/keys
     */
    @PostMapping("/{userId}/keys")
    public ResponseEntity<ApiKeyResponse> generateApiKey(@PathVariable("userId") Long userId) {
        ApiKeyResponse response = customerPlanService.generateApiKey(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3. Validate an API Key (Called internally by API Gateway)
     * POST /api/v1/customers/keys/validate
     */
    @PostMapping("/keys/validate")
    public ResponseEntity<ValidateKeyResponse> validateKey(@RequestBody Map<String, String> request) {
        String rawKey = request.get("apiKey");
        ValidateKeyResponse response = customerPlanService.validateKey(rawKey);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Revoke an API Key
     * POST /api/v1/customers/keys/{keyId}/revoke
     */
    @PostMapping("/keys/{keyId}/revoke")
    public ResponseEntity<Map<String, String>> revokeApiKey(@PathVariable("keyId") Long keyId) {
        customerPlanService.revokeApiKey(keyId);
        return ResponseEntity.ok(Map.of("message", "API Key revoked successfully", "status", "REVOKED"));
    }

    /**
     * 5. Change/Upgrade User Plan
     * PUT /api/v1/customers/{userId}/plan
     */
    @PutMapping("/{userId}/plan")
    public ResponseEntity<User> changePlan(@PathVariable("userId") Long userId,
                                           @RequestBody Map<String, String> request) {
        String newPlanName = request.get("planName");
        User updatedUser = customerPlanService.changeUserPlan(userId, newPlanName);
        return ResponseEntity.ok(updatedUser);
    }
}