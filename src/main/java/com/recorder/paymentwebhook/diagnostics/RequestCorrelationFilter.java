package com.recorder.paymentwebhook.diagnostics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final ApiMetrics apiMetrics;

    public RequestCorrelationFilter(ApiMetrics apiMetrics) {
        this.apiMetrics = apiMetrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedId = request.getHeader("X-Correlation-ID");
        String correlationId = suppliedId != null && SAFE_CORRELATION_ID.matcher(suppliedId).matches()
                ? suppliedId : UUID.randomUUID().toString();
        String path = normalizePath(request.getRequestURI());
        long startNanos = System.nanoTime();
        response.setHeader("X-Correlation-ID", correlationId);
        MDC.put("correlationId", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            apiMetrics.recordRequest(request.getMethod(), path, response.getStatus(), durationNanos);
            log.info("HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(), path, response.getStatus(), durationNanos / 1_000_000.0);
            MDC.remove("correlationId");
        }
    }

    private String normalizePath(String path) {
        if (path.matches("/api/payments/[^/]+/events")) {
            return "/api/payments/{paymentReference}/events";
        }
        if (path.matches("/api/payments/[^/]+")) {
            return "/api/payments/{paymentReference}";
        }
        if (path.matches("/api/orders/[^/]+")) {
            return "/api/orders/{orderId}";
        }
        return path;
    }
}
