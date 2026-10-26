package com.metergate.customer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys", indexes = {
        // key_hash par index banayenge taaki Gateway ki verification microsecond fast ho
        @Index(name = "idx_key_hash", columnList = "key_hash", unique = true)
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User jiski yeh key hai
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Visible prefix shown in dashboard (e.g. "sk_live_9f83a...")
    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    // SHA-256 hash of the full secret key (64 hex characters)
    // NEVER store raw key in database!
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    // Status: ACTIVE, REVOKED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public ApiKey() {
    }

    public ApiKey(User user, String keyPrefix, String keyHash) {
        this.user = user;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}