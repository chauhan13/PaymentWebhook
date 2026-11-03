package com.recorder.paymentwebhook.dto;

import java.time.Instant;
import java.util.Map;

public record PaymentSummary(
        long totalPayments,
        long totalOrders,
        long totalWebhookEvents,
        long totalFulfillments,
        Map<String, Long> paymentsByStatus,
        Instant generatedAt) {
}
