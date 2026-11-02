package com.recorder.paymentwebhook.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorder.paymentwebhook.dto.OrderView;
import com.recorder.paymentwebhook.service.PaymentQueryService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final PaymentQueryService paymentQueryService;

    public OrderController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping("/{orderId}")
    public OrderView getOrder(@PathVariable Long orderId) {
        return paymentQueryService.order(orderId);
    }
}
