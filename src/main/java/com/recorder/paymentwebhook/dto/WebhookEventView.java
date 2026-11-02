package com.recorder.paymentwebhook.dto;

import java.time.Instant;

import com.recorder.paymentwebhook.entity.WebhookEvent;

public record WebhookEventView(
        Long id,
        String eventId,
        String paymentReference,
        String eventType,
        long eventSequence,
        String payload,
        Instant receivedAt,
        Instant processedAt) {
    public static WebhookEventView from(WebhookEvent event) {
        return new WebhookEventView(
                event.getId(),
                event.getEventId(),
                event.getPaymentReference(),
                event.getEventType().name(),
                event.getEventSequence(),
                event.getPayload(),
                event.getReceivedAt(),
                event.getProcessedAt());
    }
}
