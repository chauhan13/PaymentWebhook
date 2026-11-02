package com.recorder.paymentwebhook.dataseeder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.recorder.paymentwebhook.entity.Order;
import com.recorder.paymentwebhook.entity.OrderStatus;
import com.recorder.paymentwebhook.entity.Payment;
import com.recorder.paymentwebhook.entity.PaymentStatus;
import com.recorder.paymentwebhook.entity.WebhookEvent;
import com.recorder.paymentwebhook.entity.WebhookEventType;
import com.recorder.paymentwebhook.repository.OrderRepository;
import com.recorder.paymentwebhook.repository.PaymentRepository;
import com.recorder.paymentwebhook.repository.WebhookEventRepository;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);
    private static final long RANDOM_SEED = 20251027L;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final int smallDatasetSize;
    private final boolean fullDataset;

    public SampleDataSeeder(OrderRepository orderRepository,
                            PaymentRepository paymentRepository,
                            WebhookEventRepository webhookEventRepository,
                            @Value("${app.seed.count:100}") int smallDatasetSize,
                            @Value("${app.seed.full:false}") boolean fullDataset) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.smallDatasetSize = smallDatasetSize;
        this.fullDataset = fullDataset;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (orderRepository.count() > 0) {
            log.info("Sample data already exists; skipping seeding");
            return;
        }

        int count = fullDataset ? 10_000 : smallDatasetSize;
        Random random = new Random(RANDOM_SEED);
        List<Order> orders = new ArrayList<>(count);
        List<PaymentStatus> paymentStatuses = new ArrayList<>(count);
        List<PaymentStatus> statuses = List.of(
                PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.PENDING,
                PaymentStatus.REFUNDED, PaymentStatus.CREATED);
        Instant now = Instant.now();

        for (int index = 1; index <= count; index++) {
            PaymentStatus status = statuses.get(random.nextInt(statuses.size()));
            paymentStatuses.add(status);
            BigDecimal amount = BigDecimal.valueOf(500 + random.nextInt(250_000), 2);
            Order order = new Order(
                    "ord-" + String.format("%06d", index),
                    "customer-fictional-" + String.format("%06d", index),
                    amount,
                    orderStatusFor(status));
            orders.add(order);
        }
        List<Order> savedOrders = orderRepository.saveAll(orders);

        List<Payment> payments = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            Order order = savedOrders.get(index - 1);
            PaymentStatus status = paymentStatuses.get(index - 1);
            order.setStatus(orderStatusFor(status));
            payments.add(new Payment(
                    "pay-" + String.format("%06d", index),
                    order,
                    order.getAmount(),
                    status,
                    3L));
        }
        List<Payment> savedPayments = paymentRepository.saveAll(payments);

        List<WebhookEvent> events = new ArrayList<>(count * 3);
        for (int index = 1; index <= count; index++) {
            Payment payment = savedPayments.get(index - 1);
            String paymentReference = payment.getPaymentReference();
            String firstEventId = "evt-" + String.format("%06d", index) + "-1";
            Instant createdAt = now.minus(3, ChronoUnit.HOURS);
            Instant middleAt = now.minus(2, ChronoUnit.HOURS);
            Instant finalAt = now.minus(1, ChronoUnit.HOURS);
            WebhookEventType finalType = eventTypeFor(payment.getStatus());

            events.add(event(firstEventId, paymentReference, WebhookEventType.PAYMENT_CREATED, 1, createdAt));
            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                events.add(event("evt-" + String.format("%06d", index) + "-3", paymentReference,
                        WebhookEventType.PAYMENT_REFUNDED, 3, middleAt));
                events.add(event("evt-" + String.format("%06d", index) + "-2", paymentReference,
                        WebhookEventType.PAYMENT_SUCCESS, 2, finalAt));
            } else {
                String secondEventId = index % 17 == 0 ? firstEventId
                        : "evt-" + String.format("%06d", index) + "-2";
                WebhookEventType middleType = payment.getStatus() == PaymentStatus.SUCCESS && index % 5 == 0
                        ? WebhookEventType.PAYMENT_SUCCESS : WebhookEventType.PAYMENT_PENDING;
                events.add(event(secondEventId, paymentReference, middleType, 2, middleAt));
                events.add(event("evt-" + String.format("%06d", index) + "-3", paymentReference, finalType, 3, finalAt));
            }
        }
        webhookEventRepository.saveAll(events);
        log.info("Seeded {} fictional orders, {} payments, and {} webhook events (seed={})",
                count, count, events.size(), RANDOM_SEED);
    }

    private WebhookEvent event(String id, String reference, WebhookEventType type,
                               long sequence, Instant receivedAt) {
        return new WebhookEvent(id, reference, type, sequence, "{\"seeded\":true}",
                receivedAt, receivedAt.plusMillis(8));
    }

    private WebhookEventType eventTypeFor(PaymentStatus status) {
        return switch (status) {
            case CREATED -> WebhookEventType.PAYMENT_CREATED;
            case PENDING -> WebhookEventType.PAYMENT_PENDING;
            case SUCCESS -> WebhookEventType.PAYMENT_SUCCESS;
            case FAILED -> WebhookEventType.PAYMENT_FAILED;
            case REFUNDED -> WebhookEventType.PAYMENT_REFUNDED;
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
