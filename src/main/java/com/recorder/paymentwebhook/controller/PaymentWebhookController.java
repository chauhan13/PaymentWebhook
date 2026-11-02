package com.recorder.paymentwebhook.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.recorder.paymentwebhook.dto.PaymentWebhookRequest;
import com.recorder.paymentwebhook.dto.WebhookReceipt;
import com.recorder.paymentwebhook.service.PaymentWebhookService;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {
    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WebhookReceipt receive(@Valid @RequestBody PaymentWebhookRequest request) {
        return paymentWebhookService.process(request);
    }
}
