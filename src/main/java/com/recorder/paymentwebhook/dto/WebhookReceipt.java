package com.recorder.paymentwebhook.dto;

import java.time.Instant;

public record WebhookReceipt(
        String eventId,
        String paymentReference,
        String result,
        Instant receivedAt) {
}
