package com.metergate.customer.repository;

import com.metergate.customer.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    // Gateway se aane wale SHA-256 hash se key dhoondhne ke liye (Most Important Query)
    Optional<ApiKey> findByKeyHash(String keyHash);

    // Kisi user ki saari active/revoked keys fetch karne ke liye
    List<ApiKey> findByUserId(Long userId);
}