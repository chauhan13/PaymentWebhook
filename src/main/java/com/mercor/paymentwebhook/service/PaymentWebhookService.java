package com.mercor.paymentwebhook.service;

import com.mercor.paymentwebhook.dto.PaymentWebhookRequest;
import com.mercor.paymentwebhook.dto.WebhookReceipt;
import com.mercor.paymentwebhook.entity.Fulfillment;
import com.mercor.paymentwebhook.entity.Order;
import com.mercor.paymentwebhook.entity.OrderStatus;
import com.mercor.paymentwebhook.entity.Payment;
import com.mercor.paymentwebhook.entity.PaymentStatus;
import com.mercor.paymentwebhook.entity.WebhookEvent;
import com.mercor.paymentwebhook.entity.WebhookEventType;
import com.mercor.paymentwebhook.exception.PaymentNotFoundException;
import com.mercor.paymentwebhook.diagnostics.ApiMetrics;
import com.mercor.paymentwebhook.repository.FulfillmentRepository;
import com.mercor.paymentwebhook.repository.OrderRepository;
import com.mercor.paymentwebhook.repository.PaymentRepository;
import com.mercor.paymentwebhook.repository.WebhookEventRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookService {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final ProcessingCheckpoint processingCheckpoint;
    private final ApiMetrics apiMetrics;
    private final int artificialWorkUnits;

    public PaymentWebhookService(PaymentRepository paymentRepository,
                                 OrderRepository orderRepository,
                                 WebhookEventRepository webhookEventRepository,
                                 FulfillmentRepository fulfillmentRepository,
                                 ProcessingCheckpoint processingCheckpoint,
                                 ApiMetrics apiMetrics,
                                 @Value("${app.processing.artificial-work-units:2}") int artificialWorkUnits) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.processingCheckpoint = processingCheckpoint;
        this.apiMetrics = apiMetrics;
        this.artificialWorkUnits = artificialWorkUnits;
    }

    @Transactional
    public WebhookReceipt process(PaymentWebhookRequest request) {
        long startedAt = System.nanoTime();
        Instant receivedAt = Instant.now();
        log.info("Webhook processing started paymentReference={} eventId={} eventType={} sequence={} startedAt={}",
                request.paymentReference(), request.eventId(), request.eventType(),
                request.eventSequence(), receivedAt);

        boolean alreadySeen = webhookEventRepository.existsByEventId(request.eventId());
        if (alreadySeen) {
            log.warn("Previously observed event ID received again; continuing processing paymentReference={} eventId={}",
                    request.paymentReference(), request.eventId());
        }
        processingCheckpoint.afterDuplicateCheck();

        Payment payment = paymentRepository.findByPaymentReference(request.paymentReference())
                .orElseThrow(() -> new PaymentNotFoundException(request.paymentReference()));
        Order order = orderRepository.findById(payment.getOrder().getId())
                .orElseThrow(() -> new PaymentNotFoundException(request.paymentReference()));

        for (int workUnit = 0; workUnit < artificialWorkUnits; workUnit++) {
            webhookEventRepository.countByPaymentReference(request.paymentReference());
            orderRepository.findById(order.getId());
        }

        PaymentStatus nextStatus = statusFor(request.eventType());
        payment.apply(nextStatus, request.eventSequence());
        order.setStatus(orderStatusFor(nextStatus));
        if (request.eventType() == WebhookEventType.PAYMENT_SUCCESS) {
            fulfillmentRepository.save(new Fulfillment(order, payment.getPaymentReference(), "READY"));
        }

        Instant processedAt = Instant.now();
        webhookEventRepository.save(new WebhookEvent(
                request.eventId(),
                request.paymentReference(),
                request.eventType(),
                request.eventSequence(),
                request.payload(),
                receivedAt,
                processedAt));

        long durationNanos = System.nanoTime() - startedAt;
        apiMetrics.recordWebhookProcessed(durationNanos);
        log.info("Webhook processing completed paymentReference={} eventId={} status={} durationMs={} processedAt={}",
                request.paymentReference(), request.eventId(), nextStatus,
                durationNanos / 1_000_000.0, processedAt);
        return new WebhookReceipt(request.eventId(), request.paymentReference(), "PROCESSED", receivedAt);
    }

    private PaymentStatus statusFor(WebhookEventType eventType) {
        return switch (eventType) {
            case PAYMENT_CREATED -> PaymentStatus.CREATED;
            case PAYMENT_PENDING -> PaymentStatus.PENDING;
            case PAYMENT_SUCCESS -> PaymentStatus.SUCCESS;
            case PAYMENT_FAILED -> PaymentStatus.FAILED;
            case PAYMENT_REFUNDED -> PaymentStatus.REFUNDED;
        };
    }

    private OrderStatus orderStatusFor(PaymentStatus status) {
        return switch (status) {
            case CREATED -> OrderStatus.CREATED;
            case PENDING -> OrderStatus.PAYMENT_PENDING;
            case SUCCESS -> OrderStatus.PAID;
            case FAILED -> OrderStatus.PAYMENT_FAILED;
            case REFUNDED -> OrderStatus.REFUNDED;
        };
    }
}
