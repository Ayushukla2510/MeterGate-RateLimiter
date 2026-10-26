package com.metergate.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many line items belong to One Invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore // Prevents infinite recursion when converting to JSON
    private Invoice invoice;

    // Description, e.g. "PRO Plan Base Fee" or "Overage (2,400 extra requests)"
    @Column(nullable = false, length = 255)
    private String description;

    // Number of units (e.g. 1 base subscription, or 2400 extra requests)
    @Column(nullable = false)
    private long quantity;

    // Unit price in USD (DECIMAL 10,4 to support fractional cent pricing like $0.0005 per request)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice;

    // Line total: quantity * unitPrice (DECIMAL 10,2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    public InvoiceLineItem() {
    }

    public InvoiceLineItem(String description, long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}