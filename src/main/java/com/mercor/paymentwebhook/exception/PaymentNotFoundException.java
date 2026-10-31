package com.mercor.paymentwebhook.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String paymentReference) {
        super("Payment not found: " + paymentReference);
    }
}
