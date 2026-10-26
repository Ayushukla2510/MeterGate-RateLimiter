package com.metergate.customer.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plan name: FREE, PRO, ENTERPRISE
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // Base monthly price in USD (e.g. 0.00, 20.00, 99.00)
    // Always use BigDecimal for money to avoid floating-point rounding errors
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    // Per-minute rate limit (e.g. 10 for FREE, 60 for PRO, 300 for ENTERPRISE)
    @Column(name = "rate_limit_per_minute", nullable = false)
    private int rateLimitPerMinute;

    // Monthly allowed requests quota (e.g. 1000 for FREE, 50000 for PRO)
    @Column(name = "monthly_quota", nullable = false)
    private long monthlyQuota;

    // Quota policy: HARD_CAP (block at quota) vs SOFT_CAP (allow and charge overage)
    @Column(name = "quota_policy", nullable = false, length = 20)
    private String quotaPolicy;

    // Price per 1,000 extra requests past monthly quota (e.g. 0.50 for PRO)
    @Column(name = "overage_price_per_thousand", nullable = false, precision = 10, scale = 4)
    private BigDecimal overagePricePerThousand;

    // Default constructor required by JPA
    public Plan() {
    }

    public Plan(String name, BigDecimal basePrice, int rateLimitPerMinute,
                long monthlyQuota, String quotaPolicy, BigDecimal overagePricePerThousand) {
        this.name = name;
        this.basePrice = basePrice;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.monthlyQuota = monthlyQuota;
        this.quotaPolicy = quotaPolicy;
        this.overagePricePerThousand = overagePricePerThousand;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public long getMonthlyQuota() {
        return monthlyQuota;
    }

    public void setMonthlyQuota(long monthlyQuota) {
        this.monthlyQuota = monthlyQuota;
    }

    public String getQuotaPolicy() {
        return quotaPolicy;
    }

    public void setQuotaPolicy(String quotaPolicy) {
        this.quotaPolicy = quotaPolicy;
    }

    public BigDecimal getOveragePricePerThousand() {
        return overagePricePerThousand;
    }

    public void setOveragePricePerThousand(BigDecimal overagePricePerThousand) {
        this.overagePricePerThousand = overagePricePerThousand;
    }
}