package com.metergate.limiter.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "usage_counters",
    uniqueConstraints = {
        // Ek api_key ka ek period mein sirf ek hi record hoga
        @UniqueConstraint(
            name = "uk_apikey_period",
            columnNames = {"api_key_id", "period"}
        )
    },
    indexes = {
        // Gateway har request par is query se dhundhega, index se fast hogi
        @Index(name = "idx_apikey_period", columnList = "api_key_id, period")
    }
)
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kis API key ka usage track ho raha hai
    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;

    // Billing period: e.g. "2026-10" (October 2026)
    @Column(name = "period", nullable = false, length = 7)
    private String period;

    // Total requests in this billing period (within quota)
    @Column(name = "used_count", nullable = false)
    private long usedCount = 0;

    // Extra requests beyond quota (only for SOFT_CAP plans like PRO, ENTERPRISE)
    @Column(name = "overage_count", nullable = false)
    private long overageCount = 0;

    // Default constructor required by JPA/Hibernate
    public UsageCounter() {
    }

    // Constructor for creating a new record for a key + period combination
    public UsageCounter(Long apiKeyId, String period) {
        this.apiKeyId = apiKeyId;
        this.period = period;
        this.usedCount = 0;
        this.overageCount = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Long apiKeyId) { this.apiKeyId = apiKeyId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public long getUsedCount() { return usedCount; }
    public void setUsedCount(long usedCount) { this.usedCount = usedCount; }

    public long getOverageCount() { return overageCount; }
    public void setOverageCount(long overageCount) { this.overageCount = overageCount; }
}