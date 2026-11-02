package com.recorder.paymentwebhook.diagnostics;

import org.springframework.stereotype.Service;

import com.recorder.paymentwebhook.dto.DiagnosticsView;

@Service
public class DiagnosticsService {
    private final ApiMetrics apiMetrics;

    public DiagnosticsService(ApiMetrics apiMetrics) {
        this.apiMetrics = apiMetrics;
    }

    public DiagnosticsView current() {
        return new DiagnosticsView(
                apiMetrics.getRequests(),
                apiMetrics.getWebhookRequests(),
                apiMetrics.getWebhooksProcessed(),
                apiMetrics.getWebhookProcessingNanos(),
                apiMetrics.getRequestCountsByPath());
    }
}
