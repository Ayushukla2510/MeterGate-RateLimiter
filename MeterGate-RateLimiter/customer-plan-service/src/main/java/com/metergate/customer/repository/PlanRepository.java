package com.metergate.customer.repository;

import com.metergate.customer.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    // Plan name se dhoondhne ke liye (e.g. "FREE", "PRO", "ENTERPRISE")
    Optional<Plan> findByName(String name);
}