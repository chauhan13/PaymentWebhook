package com.recorder.paymentwebhook.controller;

import com.recorder.paymentwebhook.dto.PaymentView;
import com.recorder.paymentwebhook.dto.WebhookEventView;
import com.recorder.paymentwebhook.service.PaymentQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentQueryService paymentQueryService;

    public PaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping("/{paymentReference}")
    public PaymentView getPayment(@PathVariable String paymentReference) {
        return paymentQueryService.payment(paymentReference);
    }

    @GetMapping("/{paymentReference}/events")
    public List<WebhookEventView> getEvents(@PathVariable String paymentReference) {
        return paymentQueryService.events(paymentReference);
    }
}
