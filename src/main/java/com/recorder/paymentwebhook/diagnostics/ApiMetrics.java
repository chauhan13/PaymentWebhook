package com.recorder.paymentwebhook.diagnostics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

@Component
public class ApiMetrics {
    private final MeterRegistry meterRegistry;
    private final LongAdder requests = new LongAdder();
    private final LongAdder webhookRequests = new LongAdder();
    private final LongAdder webhooksProcessed = new LongAdder();
    private final LongAdder webhookProcessingNanos = new LongAdder();
    private final Map<String, LongAdder> requestsByPath = new ConcurrentHashMap<>();

    public ApiMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String method, String path, int status, long durationNanos) {
        requests.increment();
        requestsByPath.computeIfAbsent(path, ignored -> new LongAdder()).increment();
        Timer.builder("debugger.http.requests")
                .tag("method", method)
                .tag("path", path)
                .tag("status", Integer.toString(status))
                .register(meterRegistry)
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        if ("/api/webhooks/payment".equals(path)) {
            webhookRequests.increment();
        }
    }

    public void recordWebhookProcessed(long durationNanos) {
        webhooksProcessed.increment();
        webhookProcessingNanos.add(durationNanos);
        meterRegistry.counter("debugger.webhooks.processed").increment();
        meterRegistry.timer("debugger.webhooks.processing")
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public long getRequests() { return requests.sum(); }
    public long getWebhookRequests() { return webhookRequests.sum(); }
    public long getWebhooksProcessed() { return webhooksProcessed.sum(); }
    public long getWebhookProcessingNanos() { return webhookProcessingNanos.sum(); }

    public Map<String, Long> getRequestCountsByPath() {
        return requestsByPath.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> entry.getValue().sum()));
    }
}
