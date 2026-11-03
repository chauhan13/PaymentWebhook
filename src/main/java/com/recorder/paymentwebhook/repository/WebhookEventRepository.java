package com.recorder.paymentwebhook.repository;

import com.recorder.paymentwebhook.entity.WebhookEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByEventId(String eventId);
    List<WebhookEvent> findByPaymentReferenceOrderByReceivedAtAsc(String paymentReference);
    long countByPaymentReference(String paymentReference);
    void deleteByPaymentReference(String paymentReference);
}
