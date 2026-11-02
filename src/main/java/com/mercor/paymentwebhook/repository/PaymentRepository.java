package com.mercor.paymentwebhook.repository;

import com.mercor.paymentwebhook.entity.Payment;

import jakarta.persistence.LockModeType;

import java.lang.StackWalker.Option;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentReference = ?1") 
    Optional<Payment> findLockPaymentReference(String paymentReference);
    
    void deleteByPaymentReference(String paymentReference);
}
