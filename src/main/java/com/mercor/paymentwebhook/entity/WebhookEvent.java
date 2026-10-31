package com.mercor.paymentwebhook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "webhook_events", indexes = {
        @Index(name = "idx_webhook_payment_received", columnList = "payment_reference, received_at")
})
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String eventId;

    @Column(name = "payment_reference", nullable = false, length = 48)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebhookEventType eventType;

    @Column(nullable = false)
    private long eventSequence;

    @Lob
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    private Instant processedAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(String eventId, String paymentReference, WebhookEventType eventType,
                        long eventSequence, String payload, Instant receivedAt, Instant processedAt) {
        this.eventId = eventId;
        this.paymentReference = paymentReference;
        this.eventType = eventType;
        this.eventSequence = eventSequence;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getPaymentReference() { return paymentReference; }
    public WebhookEventType getEventType() { return eventType; }
    public long getEventSequence() { return eventSequence; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
}
