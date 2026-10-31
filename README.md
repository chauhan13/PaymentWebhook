# Payment Webhook Debugger

A local Spring Boot 3 / Java 17 payment webhook exercise. It models fictional orders, payments, provider notifications, and fulfillment records using Spring Data JPA and a persistent H2 file database. The application is intentionally incorrect in several places; the bug-reproduction test group asserts the desired behavior and is expected to fail until you investigate and fix the implementation yourself.

No external payment provider, cloud service, Docker, credentials, or real customer information is used.

## Requirements and start

- Java 17+
- Maven 3.8+

```bash
mvn test
mvn spring-boot:run
```

The default startup seeds 100 orders, payments, and 300 webhook-history rows. The seed uses a fixed random seed (`20251027`) and fictional identifiers. It runs only when the database has no orders.

To generate all 10,000 orders, 10,000 payments, and 30,000 webhook notifications:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=full-data
```

The full profile uses the same fixed random seed. Allow extra startup time for the larger data set.

### Reset and reseed

Stop the application first, then remove only the local H2 data files:

```bash
rm -f payment-webhook-db.mv.db payment-webhook-db.trace.db
mvn spring-boot:run
```

Use the full-data command instead of the last command to rebuild the large data set. Test runs use isolated in-memory databases and do not modify this local file database.

## API examples

The default sample data includes `pay-000001`. The generated database identity for its order is normally `1`.

### Receive a payment webhook

```bash
curl -i -X POST http://localhost:8080/api/webhooks/payment \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: local-debug-001' \
  -d '{
    "eventId": "evt-manual-success-001",
    "paymentReference": "pay-000001",
    "eventType": "PAYMENT_SUCCESS",
    "eventSequence": 50,
    "payload": "{\"source\":\"curl-debug\"}"
  }'
```

Returns `202 Accepted` when processed. Required fields and a positive sequence are validated; malformed requests return `400`, and unknown payments return `404`.

### Read payment and order state

```bash
curl -i http://localhost:8080/api/payments/pay-000001
curl -i http://localhost:8080/api/orders/1
```

### Read webhook history

```bash
curl -i http://localhost:8080/api/payments/pay-000001/events
```

### Read payment summary and request diagnostics

```bash
curl -i http://localhost:8080/api/diagnostics/payment-summary
curl -i http://localhost:8080/api/diagnostics
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/metrics/debugger.webhooks.processing
```

The summary endpoint is deliberately query-heavy. Timing measurements are machine-dependent; use SQL/query counts and fulfillment counts as deterministic comparisons.

## Debugging tests

The normal test command runs the passing smoke tests and excludes the explicitly tagged reproductions:

```bash
mvn test
```

Run the reproduction suite before making changes to see the expected failures:

```bash
mvn -Pbug-reproduction -Dtest=PaymentWebhookBugReproductionTest test
```

To run the smoke tests and bug reproductions together:

```bash
mvn -Pbug-reproduction test
```

The reproduction tests assert the intended business outcomes, so they are expected to fail on the starter implementation. The concurrent duplicate test coordinates both requests with latches at the duplicate-check boundary; it does not depend on sleeps.

## Intentional investigation areas

- **Duplicate delivery:** an existing event-ID check is logged but does not stop processing. Successful deliveries can create multiple fulfillment rows.
- **Concurrency:** the check and fulfillment creation have no atomic safeguard. The test gate makes both concurrent requests pass the check before either continues.
- **Out-of-order/state transitions:** every notification overwrites the payment and order state regardless of sequence or prior state.
- **Query count:** payment summary iterates all payments and issues repeated order, event-count, and fulfillment-count queries.
- **Processing latency:** webhook processing performs configurable redundant reads. It uses no artificial sleep.
- **Seed history:** data includes duplicate event IDs, multiple event IDs for a payment, repeated successes, and an older success notification recorded after a refund.

These are exercise prompts, not recommendations for production payment processing.

## Logging and performance comparison

Each HTTP response includes an `X-Correlation-ID`; a safe caller-provided value is reused, otherwise one is generated. Logs include the correlation ID, payment reference, event ID, processing timestamps, and elapsed time. The diagnostics endpoint keeps process-local request counts and webhook duration totals.

Enable verbose Hibernate SQL and Hibernate query statistics with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=sql-debug
```

You can combine Spring profiles, for example:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=full-data,sql-debug
```

Compare the same API call, `debugger.http.requests` / `debugger.webhooks.processing` Actuator metrics, generated SQL, `GET /api/diagnostics/payment-summary` query count, and fulfillment counts before and after your changes. The test suite also has a deterministic summary-query budget assertion.

## Project layout

```text
src/main/java/com/mercor/paymentwebhook/PaymentWebhookApplication.java
src/main/java/com/mercor/paymentwebhook/config/DefaultProcessingCheckpoint.java
src/main/java/com/mercor/paymentwebhook/controller/DiagnosticsController.java
src/main/java/com/mercor/paymentwebhook/controller/OrderController.java
src/main/java/com/mercor/paymentwebhook/controller/PaymentController.java
src/main/java/com/mercor/paymentwebhook/controller/PaymentWebhookController.java
src/main/java/com/mercor/paymentwebhook/dataseeder/SampleDataSeeder.java
src/main/java/com/mercor/paymentwebhook/diagnostics/ApiMetrics.java
src/main/java/com/mercor/paymentwebhook/diagnostics/DiagnosticsService.java
src/main/java/com/mercor/paymentwebhook/diagnostics/RequestCorrelationFilter.java
src/main/java/com/mercor/paymentwebhook/dto/DiagnosticsView.java
src/main/java/com/mercor/paymentwebhook/dto/OrderView.java
src/main/java/com/mercor/paymentwebhook/dto/PaymentSummary.java
src/main/java/com/mercor/paymentwebhook/dto/PaymentView.java
src/main/java/com/mercor/paymentwebhook/dto/PaymentWebhookRequest.java
src/main/java/com/mercor/paymentwebhook/dto/WebhookEventView.java
src/main/java/com/mercor/paymentwebhook/dto/WebhookReceipt.java
src/main/java/com/mercor/paymentwebhook/entity/Fulfillment.java
src/main/java/com/mercor/paymentwebhook/entity/Order.java
src/main/java/com/mercor/paymentwebhook/entity/OrderStatus.java
src/main/java/com/mercor/paymentwebhook/entity/Payment.java
src/main/java/com/mercor/paymentwebhook/entity/PaymentStatus.java
src/main/java/com/mercor/paymentwebhook/entity/WebhookEvent.java
src/main/java/com/mercor/paymentwebhook/entity/WebhookEventType.java
src/main/java/com/mercor/paymentwebhook/exception/ApiExceptionHandler.java
src/main/java/com/mercor/paymentwebhook/exception/PaymentNotFoundException.java
src/main/java/com/mercor/paymentwebhook/repository/FulfillmentRepository.java
src/main/java/com/mercor/paymentwebhook/repository/OrderRepository.java
src/main/java/com/mercor/paymentwebhook/repository/PaymentRepository.java
src/main/java/com/mercor/paymentwebhook/repository/WebhookEventRepository.java
src/main/java/com/mercor/paymentwebhook/service/PaymentQueryService.java
src/main/java/com/mercor/paymentwebhook/service/PaymentWebhookService.java
src/main/java/com/mercor/paymentwebhook/service/ProcessingCheckpoint.java
src/main/resources/application.properties
src/main/resources/application-full-data.properties
src/main/resources/application-sql-debug.properties
src/test/java/com/mercor/paymentwebhook/PaymentProcessingSmokeTest.java
src/test/java/com/mercor/paymentwebhook/PaymentWebhookBugReproductionTest.java
```

Open the project folder in VS Code, run the Maven commands in Terminal.app, and inspect the service and test failures. After you make your own fixes, rerun both test commands and compare the diagnostics before committing your corrected implementation with GitHub Desktop.
