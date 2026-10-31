package com.mercor.paymentwebhook.repository;

import com.mercor.paymentwebhook.entity.WebhookEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByEventId(String eventId);
    List<WebhookEvent> findByPaymentReferenceOrderByReceivedAtAsc(String paymentReference);
    long countByPaymentReference(String paymentReference);
    void deleteByPaymentReference(String paymentReference);
}
