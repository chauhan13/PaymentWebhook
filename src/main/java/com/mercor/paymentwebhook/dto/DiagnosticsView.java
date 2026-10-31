package com.mercor.paymentwebhook.dto;

import java.util.Map;

public record DiagnosticsView(
        long requests,
        long webhookRequests,
        long webhooksProcessed,
        long cumulativeWebhookProcessingNanos,
        Map<String, Long> requestCountsByPath) {
}
