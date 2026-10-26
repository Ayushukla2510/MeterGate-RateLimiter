package com.metergate.customer.config;

import com.metergate.customer.entity.Plan;
import com.metergate.customer.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    // Constructor injection
    public DataInitializer(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        // Table khali hai ya nahi check karo
        if (planRepository.count() == 0) {
            // 1. FREE Plan: $0 base, 10 req/min limit, 1,000 monthly quota, HARD_CAP
            Plan freePlan = new Plan(
                    "FREE",
                    new BigDecimal("0.00"),
                    10,
                    1000L,
                    "HARD_CAP",
                    new BigDecimal("0.0000")
            );

            // 2. PRO Plan: $20 base, 60 req/min limit, 50,000 monthly quota, SOFT_CAP, $0.50 per 1k extra requests
            Plan proPlan = new Plan(
                    "PRO",
                    new BigDecimal("20.00"),
                    60,
                    50000L,
                    "SOFT_CAP",
                    new BigDecimal("0.5000")
            );

            // 3. ENTERPRISE Plan: $99 base, 300 req/min limit, 500,000 monthly quota, SOFT_CAP, $0.20 per 1k extra requests
            Plan enterprisePlan = new Plan(
                    "ENTERPRISE",
                    new BigDecimal("99.00"),
                    300,
                    500000L,
                    "SOFT_CAP",
                    new BigDecimal("0.2000")
            );

            // MySQL database mein teeno plans save kar do
            planRepository.save(freePlan);
            planRepository.save(proPlan);
            planRepository.save(enterprisePlan);

            System.out.println(">>> [MeterGate] Successfully seeded FREE, PRO, and ENTERPRISE plans into MySQL!");
        }
    }
}