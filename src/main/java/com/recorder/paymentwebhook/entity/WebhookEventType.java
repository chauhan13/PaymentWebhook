package com.recorder.paymentwebhook.entity;

public enum WebhookEventType {
    PAYMENT_CREATED,
    PAYMENT_PENDING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED
}
