package com.recorder.paymentwebhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.recorder.paymentwebhook.dto.PaymentWebhookRequest;
import com.recorder.paymentwebhook.entity.Order;
import com.recorder.paymentwebhook.entity.OrderStatus;
import com.recorder.paymentwebhook.entity.Payment;
import com.recorder.paymentwebhook.entity.PaymentStatus;
import com.recorder.paymentwebhook.entity.WebhookEventType;
import com.recorder.paymentwebhook.repository.FulfillmentRepository;
import com.recorder.paymentwebhook.repository.OrderRepository;
import com.recorder.paymentwebhook.repository.PaymentRepository;
import com.recorder.paymentwebhook.service.PaymentWebhookService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-smoke;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.count=10",
        "app.processing.artificial-work-units=0"
})
class PaymentProcessingSmokeTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private PaymentWebhookService paymentWebhookService;

    @Test
    void normalSuccessfulPaymentCreatesPaymentAndFulfillment() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Order order = orderRepository.save(new Order(
                "smoke-order-" + suffix, "fictional-smoke-customer",
                new BigDecimal("25.00"), OrderStatus.CREATED));
        Payment payment = paymentRepository.save(new Payment(
                "smoke-payment-" + suffix, order, new BigDecimal("25.00"),
                PaymentStatus.CREATED, 0));

        paymentWebhookService.process(new PaymentWebhookRequest(
                "smoke-event-" + suffix, payment.getPaymentReference(),
                WebhookEventType.PAYMENT_SUCCESS, 1, "{\"test\":true}"));

        assertThat(paymentRepository.findByPaymentReference(payment.getPaymentReference()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(fulfillmentRepository.countByOrder_Id(order.getId())).isEqualTo(1);
    }

    @Test
    void summaryEndpointAndCorrelationHeaderAreAvailable() throws Exception {
        mockMvc.perform(get("/api/diagnostics/payment-summary")
                        .header("X-Correlation-ID", "smoke-correlation-01"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "smoke-correlation-01"))
                .andExpect(jsonPath("$.totalPayments").isNumber())
                .andExpect(jsonPath("$.paymentsByStatus").isMap());
    }

    @Test
    void invalidWebhookReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"","paymentReference":"","eventType":"PAYMENT_SUCCESS","eventSequence":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"));
    }
}
