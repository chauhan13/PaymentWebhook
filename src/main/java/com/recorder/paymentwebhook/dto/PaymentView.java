package com.recorder.paymentwebhook.dto;

import com.recorder.paymentwebhook.entity.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentView(
        Long id,
        String paymentReference,
        Long orderId,
        BigDecimal amount,
        String status,
        long lastEventSequence,
        long fulfillmentCount,
        Instant createdAt,
        Instant updatedAt) {
    public static PaymentView from(Payment payment, long fulfillmentCount) {
        return new PaymentView(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getLastEventSequence(),
                fulfillmentCount,
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
