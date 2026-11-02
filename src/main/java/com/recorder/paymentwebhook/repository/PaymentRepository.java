package com.recorder.paymentwebhook.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.recorder.paymentwebhook.entity.Payment;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query ("SELECT p FROM Payment p WHERE p.paymentReference = ?1")
    Optional<Payment> findByPaymentReferenceForUpdate(String paymentReference);
    
    void deleteByPaymentReference(String paymentReference);
}
