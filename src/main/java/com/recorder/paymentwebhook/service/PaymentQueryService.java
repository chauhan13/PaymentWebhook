package com.recorder.paymentwebhook.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorder.paymentwebhook.dto.OrderView;
import com.recorder.paymentwebhook.dto.PaymentSummary;
import com.recorder.paymentwebhook.dto.PaymentView;
import com.recorder.paymentwebhook.dto.WebhookEventView;
import com.recorder.paymentwebhook.entity.Order;
import com.recorder.paymentwebhook.entity.Payment;
import com.recorder.paymentwebhook.exception.PaymentNotFoundException;
import com.recorder.paymentwebhook.repository.FulfillmentRepository;
import com.recorder.paymentwebhook.repository.OrderRepository;
import com.recorder.paymentwebhook.repository.PaymentRepository;
import com.recorder.paymentwebhook.repository.WebhookEventRepository;

@Service
public class PaymentQueryService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository,
                               OrderRepository orderRepository,
                               WebhookEventRepository webhookEventRepository,
                               FulfillmentRepository fulfillmentRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentView payment(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(paymentReference));
        return PaymentView.from(payment, fulfillmentRepository.countByOrder_Id(payment.getOrder().getId()));
    }

    @Transactional(readOnly = true)
    public OrderView order(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Order not found: " + orderId));
        return OrderView.from(order);
    }

    @Transactional(readOnly = true)
    public List<WebhookEventView> events(String paymentReference) {
        if (paymentRepository.findByPaymentReference(paymentReference).isEmpty()) {
            throw new PaymentNotFoundException(paymentReference);
        }
        return webhookEventRepository.findByPaymentReferenceOrderByReceivedAtAsc(paymentReference)
                .stream().map(WebhookEventView::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary() {
        List<Payment> payments = paymentRepository.findAll();
        Map<String, Long> countsByStatus = new java.util.LinkedHashMap<>();
        for (Payment payment : payments) {
            countsByStatus.merge(payment.getStatus().name(), 1L, Long::sum);
        }
        return new PaymentSummary(
                payments.size(),
                orderRepository.count(),
                webhookEventRepository.count(),
                fulfillmentRepository.count(),
                countsByStatus,
                Instant.now());
    }
}
