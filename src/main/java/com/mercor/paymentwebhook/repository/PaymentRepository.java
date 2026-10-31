package com.mercor.paymentwebhook.repository;

import com.mercor.paymentwebhook.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    void deleteByPaymentReference(String paymentReference);
}
