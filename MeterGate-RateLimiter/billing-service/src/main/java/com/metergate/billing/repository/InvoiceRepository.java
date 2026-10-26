package com.metergate.billing.repository;

import com.metergate.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Check if an invoice was already generated for this user and period (e.g. user 1, "2026-10")
    Optional<Invoice> findByUserIdAndPeriod(Long userId, String period);

    // Get all historical invoices for a customer
    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);
}