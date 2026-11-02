package com.recorder.paymentwebhook.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorder.paymentwebhook.diagnostics.DiagnosticsService;
import com.recorder.paymentwebhook.dto.DiagnosticsView;
import com.recorder.paymentwebhook.dto.PaymentSummary;
import com.recorder.paymentwebhook.service.PaymentQueryService;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {
    private final PaymentQueryService paymentQueryService;
    private final DiagnosticsService diagnosticsService;

    public DiagnosticsController(PaymentQueryService paymentQueryService, DiagnosticsService diagnosticsService) {
        this.paymentQueryService = paymentQueryService;
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/payment-summary")
    public PaymentSummary paymentSummary() {
        return paymentQueryService.summary();
    }

    @GetMapping
    public DiagnosticsView diagnostics() {
        return diagnosticsService.current();
    }
}
