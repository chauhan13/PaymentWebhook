package com.recorder.paymentwebhook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 48)
    private String paymentReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false)
    private long lastEventSequence;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String paymentReference, Order order, BigDecimal amount, PaymentStatus status,
                   long lastEventSequence) {
        this.paymentReference = paymentReference;
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.lastEventSequence = lastEventSequence;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getPaymentReference() { return paymentReference; }
    public Order getOrder() { return order; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public long getLastEventSequence() { return lastEventSequence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void apply(PaymentStatus status, long sequence) {
        this.status = status;
        this.lastEventSequence = sequence;
        this.updatedAt = Instant.now();
    }
}
