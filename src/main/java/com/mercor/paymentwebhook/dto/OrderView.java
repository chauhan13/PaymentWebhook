package com.mercor.paymentwebhook.dto;

import com.mercor.paymentwebhook.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderView(
        Long id,
        String orderReference,
        String customerId,
        BigDecimal amount,
        String status,
        String paymentReference,
        Instant createdAt,
        Instant updatedAt) {
    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerId(),
                order.getAmount(),
                order.getStatus().name(),
                order.getPayment() == null ? null : order.getPayment().getPaymentReference(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
