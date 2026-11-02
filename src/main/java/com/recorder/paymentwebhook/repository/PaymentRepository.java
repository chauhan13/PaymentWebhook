package com.recorder.paymentwebhook.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.recorder.paymentwebhook.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    void deleteByPaymentReference(String paymentReference);
}
