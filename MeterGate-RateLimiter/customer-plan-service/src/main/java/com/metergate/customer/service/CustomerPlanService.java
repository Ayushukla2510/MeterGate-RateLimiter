package com.metergate.customer.service;

import com.metergate.customer.dto.ApiKeyResponse;
import com.metergate.customer.dto.CreateUserRequest;
import com.metergate.customer.dto.ValidateKeyResponse;
import com.metergate.customer.entity.ApiKey;
import com.metergate.customer.entity.Plan;
import com.metergate.customer.entity.User;
import com.metergate.customer.repository.ApiKeyRepository;
import com.metergate.customer.repository.PlanRepository;
import com.metergate.customer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class CustomerPlanService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ApiKeyRepository apiKeyRepository;

    // Cryptographically strong random generator for API keys
    private final SecureRandom secureRandom = new SecureRandom();

    // Constructor injection (Best practice: no @Autowired field injection)
    public CustomerPlanService(UserRepository userRepository,
                               PlanRepository planRepository,
                               ApiKeyRepository apiKeyRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * 1. Register a new user with a chosen plan (defaults to FREE).
     */
    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email already exists: " + request.email());
        }

        String targetPlanName = (request.planName() != null && !request.planName().isBlank())
                ? request.planName().toUpperCase()
                : "FREE";

        Plan plan = planRepository.findByName(targetPlanName)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + targetPlanName));

        User user = new User(request.name(), request.email(), plan);
        return userRepository.save(user);
    }

    /**
     * 2. Generate a Stripe-style API Key (sk_live_...) for a user.
     * The raw key is returned ONLY ONCE in the response.
     * Only the SHA-256 hash is saved to the database.
     */
    @Transactional
    public ApiKeyResponse generateApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Generate 24 random bytes (produces ~32 URL-safe chars)
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String randomEntropy = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Final raw key format: sk_live_<random entropy>
        String rawKey = "sk_live_" + randomEntropy;

        // Visible prefix for dashboard identification (first 16 chars, e.g. "sk_live_aB3dEf")
        String keyPrefix = rawKey.substring(0, Math.min(rawKey.length(), 16));

        // Compute SHA-256 hash (64 hex characters)
        String keyHash = hashKey(rawKey);

        // Save entity to DB (raw key is NEVER persisted)
        ApiKey apiKey = new ApiKey(user, keyPrefix, keyHash);
        ApiKey savedKey = apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
                savedKey.getId(),
                user.getId(),
                rawKey, // Exposed only this one time!
                keyPrefix,
                savedKey.getStatus(),
                savedKey.getCreatedAt()
        );
    }

    /**
     * 3. Validate key when Gateway calls.
     * Gateway passes the raw key. We hash it and do a fast indexed DB lookup.
     */
    @Transactional(readOnly = true)
    public ValidateKeyResponse validateKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return invalidResponse();
        }

        String keyHash = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            return invalidResponse();
        }

        ApiKey apiKey = apiKeyOpt.get();

        // If key is revoked, reject it
        if (!"ACTIVE".equalsIgnoreCase(apiKey.getStatus())) {
            return new ValidateKeyResponse(
                    false,
                    apiKey.getId(),
                    apiKey.getUser().getId(),
                    apiKey.getUser().getPlan().getName(),
                    apiKey.getUser().getPlan().getRateLimitPerMinute(),
                    apiKey.getUser().getPlan().getMonthlyQuota(),
                    apiKey.getUser().getPlan().getQuotaPolicy(),
                    apiKey.getUser().getPlan().getOveragePricePerThousand(),
                    "REVOKED"
            );
        }

        // Active key -> Return limits and user info to Gateway
        Plan plan = apiKey.getUser().getPlan();
        return new ValidateKeyResponse(
                true,
                apiKey.getId(),
                apiKey.getUser().getId(),
                plan.getName(),
                plan.getRateLimitPerMinute(),
                plan.getMonthlyQuota(),
                plan.getQuotaPolicy(),
                plan.getOveragePricePerThousand(),
                "ACTIVE"
        );
    }

    /**
     * 4. Revoke an API Key.
     */
    @Transactional
    public void revokeApiKey(Long keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found: " + keyId));
        apiKey.setStatus("REVOKED");
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
    }

    /**
     * 5. Change a user's plan (e.g. FREE -> PRO).
     */
    @Transactional
    public User changeUserPlan(Long userId, String newPlanName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Plan newPlan = planRepository.findByName(newPlanName.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + newPlanName));

        user.setPlan(newPlan);
        return userRepository.save(user);
    }

    /**
     * Helper: Compute SHA-256 hash of a string, formatted as 64-character hex.
     */
    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private ValidateKeyResponse invalidResponse() {
        return new ValidateKeyResponse(
                false, null, null, null, 0, 0, null, null, "INVALID"
        );
    }

    /**
     * Fetch user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }
}