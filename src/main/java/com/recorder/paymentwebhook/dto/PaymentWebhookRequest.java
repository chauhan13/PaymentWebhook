package com.recorder.paymentwebhook.dto;

import com.recorder.paymentwebhook.entity.WebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentWebhookRequest(
        @NotBlank String eventId,
        @NotBlank String paymentReference,
        @NotNull WebhookEventType eventType,
        @Positive long eventSequence,
        String payload) {
}
