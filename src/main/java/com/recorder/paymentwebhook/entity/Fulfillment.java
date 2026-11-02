package com.recorder.paymentwebhook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fulfillments")
public class Fulfillment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 48)
    private String paymentReference;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Fulfillment() {
    }

    public Fulfillment(Order order, String paymentReference, String status) {
        this.order = order;
        this.paymentReference = paymentReference;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public String getPaymentReference() { return paymentReference; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
