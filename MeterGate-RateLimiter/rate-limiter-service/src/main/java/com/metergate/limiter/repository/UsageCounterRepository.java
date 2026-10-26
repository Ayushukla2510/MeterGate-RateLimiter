package com.metergate.limiter.repository;

import com.metergate.limiter.entity.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {

    // 1. Find usage record for a specific API key in a specific billing period
    Optional<UsageCounter> findByApiKeyIdAndPeriod(Long apiKeyId, String period);

    // 2. ATOMIC increment of used_count by 1
    // This single SQL statement is race-condition-free at the database level
    @Modifying
    @Query("UPDATE UsageCounter u SET u.usedCount = u.usedCount + 1 " +
            "WHERE u.apiKeyId = :apiKeyId AND u.period = :period")
    int incrementUsedCount(@Param("apiKeyId") Long apiKeyId,
                           @Param("period") String period);

    // 3. ATOMIC increment of overage_count by 1 (for PRO/ENTERPRISE soft cap plans)
    @Modifying
    @Query("UPDATE UsageCounter u SET u.overageCount = u.overageCount + 1 " +
            "WHERE u.apiKeyId = :apiKeyId AND u.period = :period")
    int incrementOverageCount(@Param("apiKeyId") Long apiKeyId,
                              @Param("period") String period);
}