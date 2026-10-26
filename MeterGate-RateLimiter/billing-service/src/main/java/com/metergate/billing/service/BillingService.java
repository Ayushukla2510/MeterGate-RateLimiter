package com.metergate.billing.service;

import com.metergate.billing.client.*;
import com.metergate.billing.entity.Invoice;
import com.metergate.billing.entity.InvoiceLineItem;
import com.metergate.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerServiceClient customerServiceClient;
    private final RateLimiterClient rateLimiterClient;

    // Constructor injection for repository and both Feign clients
    public BillingService(InvoiceRepository invoiceRepository,
                          CustomerServiceClient customerServiceClient,
                          RateLimiterClient rateLimiterClient) {
        this.invoiceRepository = invoiceRepository;
        this.customerServiceClient = customerServiceClient;
        this.rateLimiterClient = rateLimiterClient;
    }

    /**
     * Generates an itemized monthly invoice for a user.
     * Idempotent: If an invoice already exists for (userId, period), returns the existing one.
     */
    @Transactional
    public Invoice generateInvoice(Long userId, Long apiKeyId, String period) {

        // 1. Check if invoice already exists for this period (Idempotency)
        Optional<Invoice> existingInvoice = invoiceRepository.findByUserIdAndPeriod(userId, period);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        // 2. Fetch User and Plan info via OpenFeign from customer-plan-service
        UserDto user = customerServiceClient.getUserById(userId);
        PlanDto plan = user.plan();

        // 3. Fetch monthly usage via OpenFeign from rate-limiter-service
        UsageDto usage = rateLimiterClient.getUsage(apiKeyId, period);

        // 4. Initialize Invoice
        BigDecimal totalAmount = BigDecimal.ZERO;
        Invoice invoice = new Invoice(userId, period, BigDecimal.ZERO, "GENERATED");

        // Line Item 1: Base Subscription Fee
        BigDecimal basePrice = plan.basePrice();
        String baseDesc = String.format("%s Plan Subscription (%d included requests)",
                plan.name(), plan.monthlyQuota());
        InvoiceLineItem baseItem = new InvoiceLineItem(baseDesc, 1, basePrice, basePrice);
        invoice.addLineItem(baseItem);
        totalAmount = totalAmount.add(basePrice);

        // Line Item 2: Overage Fee (if any extra requests beyond quota)
        if (usage.overageCount() > 0 && plan.overagePricePerThousand().compareTo(BigDecimal.ZERO) > 0) {
            // Price per single request = overagePricePerThousand / 1000
            BigDecimal pricePerUnit = plan.overagePricePerThousand()
                    .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);

            // Overage cost = overageCount * pricePerUnit, rounded to 2 decimal cents
            BigDecimal overageCost = pricePerUnit.multiply(new BigDecimal(usage.overageCount()))
                    .setScale(2, RoundingMode.HALF_UP);

            String overageDesc = String.format("API Overage (%d extra requests @ $%s/1k)",
                    usage.overageCount(), plan.overagePricePerThousand().toPlainString());

            InvoiceLineItem overageItem = new InvoiceLineItem(overageDesc, usage.overageCount(), pricePerUnit, overageCost);
            invoice.addLineItem(overageItem);
            totalAmount = totalAmount.add(overageCost);
        }

        // 5. Finalize total and save invoice with line items
        invoice.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        return invoiceRepository.save(invoice);
    }

    /**
     * Get an invoice by its ID
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
    }

    /**
     * Get all invoices for a user
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByUserId(Long userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}