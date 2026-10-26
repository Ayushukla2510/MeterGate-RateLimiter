package com.metergate.customer.repository;

import com.metergate.customer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Check karne ke liye ki yeh email already registered toh nahi hai
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}