package com.recorder.paymentwebhook.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    REFUNDED
}
