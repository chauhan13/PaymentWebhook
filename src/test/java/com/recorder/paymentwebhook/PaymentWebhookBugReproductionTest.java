package com.recorder.paymentwebhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import com.recorder.paymentwebhook.dto.PaymentWebhookRequest;
import com.recorder.paymentwebhook.entity.Order;
import com.recorder.paymentwebhook.entity.OrderStatus;
import com.recorder.paymentwebhook.entity.Payment;
import com.recorder.paymentwebhook.entity.PaymentStatus;
import com.recorder.paymentwebhook.entity.WebhookEventType;
import com.recorder.paymentwebhook.repository.FulfillmentRepository;
import com.recorder.paymentwebhook.repository.OrderRepository;
import com.recorder.paymentwebhook.repository.PaymentRepository;
import com.recorder.paymentwebhook.repository.WebhookEventRepository;
import com.recorder.paymentwebhook.service.PaymentQueryService;
import com.recorder.paymentwebhook.service.PaymentWebhookService;

@Tag("bug-reproduction")
@SpringBootTest
@AutoConfigureMockMvc
@Import(PaymentWebhookBugReproductionTest.RaceTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-bugs;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.seed.count=10",
        "app.processing.artificial-work-units=0"
})
class PaymentWebhookBugReproductionTest {
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private PaymentWebhookService paymentWebhookService;
    @Autowired private PaymentQueryService paymentQueryService;
    @Autowired private MockMvc mockMvc;
    @Autowired private RaceGate raceGate;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private TransactionTemplate transactionTemplate;

    private Order order;
    private Payment payment;

    @BeforeEach
    void createPaymentFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        order = orderRepository.save(new Order(
                "bug-order-" + suffix, "fictional-bug-customer",
                new BigDecimal("49.95"), OrderStatus.CREATED));
        payment = paymentRepository.save(new Payment(
                "bug-payment-" + suffix, order, new BigDecimal("49.95"),
                PaymentStatus.CREATED, 0));
    }

    @AfterEach
    void removePaymentFixture() {
        if (payment == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            webhookEventRepository.deleteByPaymentReference(payment.getPaymentReference());
            fulfillmentRepository.deleteAll(fulfillmentRepository.findByOrder_Id(order.getId()));
            paymentRepository.delete(payment);
            orderRepository.delete(order);
        });
        raceGate.disarm();
    }

    @Test
    void repeatedDeliveryShouldCreateOnlyOneFulfillment() {
        PaymentWebhookRequest request = request("same-event-id", WebhookEventType.PAYMENT_SUCCESS, 1);
        paymentWebhookService.process(request);
        paymentWebhookService.process(request);

        assertThat(fulfillmentRepository.countByOrder_Id(order.getId())).isEqualTo(1);
    }

    @Test
    void simultaneousDuplicateRequestsShouldCreateOnlyOneFulfillment() throws Exception {
        raceGate.arm(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            String body = """
                    {"eventId":"concurrent-event-id","paymentReference":"%s",
                     "eventType":"PAYMENT_SUCCESS","eventSequence":1}
                    """.formatted(payment.getPaymentReference());
            Future<Integer> first = executor.submit(() -> mockMvc.perform(post("/api/webhooks/payment")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus());
            Future<Integer> second = executor.submit(() -> mockMvc.perform(post("/api/webhooks/payment")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus());
            assertThat(raceGate.awaitBoth(10, TimeUnit.SECONDS)).isTrue();
            raceGate.release();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(202);
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(202);

            assertThat(fulfillmentRepository.countByOrder_Id(order.getId())).isEqualTo(1);
        } finally {
            raceGate.release();
            executor.shutdownNow();
        }
    }

    @Test
    void delayedSuccessAfterRefundShouldNotRestorePaymentToSuccess() {
        paymentWebhookService.process(request("refund-event-id", WebhookEventType.PAYMENT_REFUNDED, 20));
        paymentWebhookService.process(request("delayed-success-event-id", WebhookEventType.PAYMENT_SUCCESS, 19));

        assertThat(paymentRepository.findByPaymentReference(payment.getPaymentReference()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void differentSuccessEventIdsShouldNotDuplicateFulfillment() {
        paymentWebhookService.process(request("success-attempt-a", WebhookEventType.PAYMENT_SUCCESS, 1));
        paymentWebhookService.process(request("success-attempt-b", WebhookEventType.PAYMENT_SUCCESS, 2));

        assertThat(fulfillmentRepository.countByOrder_Id(order.getId())).isEqualTo(1);
    }

    @Test
    void successfulPaymentShouldNotRegressToPending() {
        paymentWebhookService.process(request("transition-success", WebhookEventType.PAYMENT_SUCCESS, 1));
        paymentWebhookService.process(request("transition-pending", WebhookEventType.PAYMENT_PENDING, 2));

        assertThat(paymentRepository.findByPaymentReference(payment.getPaymentReference()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void paymentSummaryShouldStayBelowQueryCountBudget() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        paymentQueryService.summary();

        assertThat(statistics.getPrepareStatementCount()).isLessThan(15);
    }

    private PaymentWebhookRequest request(String eventId, WebhookEventType type, long sequence) {
        return new PaymentWebhookRequest(eventId, payment.getPaymentReference(), type, sequence, "{\"test\":true}");
    }

    @TestConfiguration
    static class RaceTestConfiguration {
        @Bean
        @Primary
        RaceGate raceGate() {
            return new RaceGate();
        }
    }

    static class RaceGate implements com.recorder.paymentwebhook.service.ProcessingCheckpoint {
        private final AtomicBoolean armed = new AtomicBoolean();
        private volatile CountDownLatch arrived = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);

        void arm(int parties) {
            arrived = new CountDownLatch(parties);
            release = new CountDownLatch(1);
            armed.set(true);
        }

        boolean awaitBoth(long timeout, TimeUnit unit) throws InterruptedException {
            return arrived.await(timeout, unit);
        }

        void release() {
            armed.set(false);
            release.countDown();
        }

        void disarm() {
            release();
        }

        @Override
        public void afterDuplicateCheck() {
            if (!armed.get()) {
                return;
            }
            arrived.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Race reproduction gate timed out");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Race reproduction gate interrupted", exception);
            }
        }
    }
}
