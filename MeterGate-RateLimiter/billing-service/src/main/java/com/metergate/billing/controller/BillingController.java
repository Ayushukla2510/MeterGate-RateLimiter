package com.metergate.billing.controller;

import com.metergate.billing.entity.Invoice;
import com.metergate.billing.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;

    // Constructor injection
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Trigger invoice generation on-demand.
     * POST /billing/invoices/generate?userId=1&apiKeyId=1&period=2026-10
     */
    @PostMapping("/invoices/generate")
    public ResponseEntity<Invoice> generateInvoice(
            @RequestParam("userId") Long userId,
            @RequestParam("apiKeyId") Long apiKeyId,
            @RequestParam("period") String period) {

        Invoice invoice = billingService.generateInvoice(userId, apiKeyId, period);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    /**
     * Get a specific invoice by its ID
     * GET /billing/invoices/{id}
     */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable("id") Long id) {
        Invoice invoice = billingService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Get all historical invoices for a specific user
     * GET /billing/users/{userId}/invoices
     */
    @GetMapping("/users/{userId}/invoices")
    public ResponseEntity<List<Invoice>> getInvoicesByUserId(@PathVariable("userId") Long userId) {
        List<Invoice> invoices = billingService.getInvoicesByUserId(userId);
        return ResponseEntity.ok(invoices);
    }
}