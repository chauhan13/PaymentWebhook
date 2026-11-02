package com.mercor.paymentwebhook.service;

import com.mercor.paymentwebhook.dto.PaymentSummary;
import com.mercor.paymentwebhook.dto.OrderView;
import com.mercor.paymentwebhook.dto.PaymentView;
import com.mercor.paymentwebhook.dto.WebhookEventView;
import com.mercor.paymentwebhook.entity.Order;
import com.mercor.paymentwebhook.entity.Payment;
import com.mercor.paymentwebhook.exception.PaymentNotFoundException;
import com.mercor.paymentwebhook.repository.FulfillmentRepository;
import com.mercor.paymentwebhook.repository.OrderRepository;
import com.mercor.paymentwebhook.repository.PaymentRepository;
import com.mercor.paymentwebhook.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
