# Ecomera Payment Service

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Gateway](https://img.shields.io/badge/Gateway-Mock%20%2F%20Stripe-008CDD?logo=stripe&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-Integrated-6DB33F)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative&logoColor=white)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ecomera-payment-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ecomera-payment-service)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=ecomera-payment-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ecomera-payment-service)

Payment processing microservice for the Ecomera ecosystem. Handles payment intents, refunds, and webhook events. Ships with a **mock gateway** by default — no API keys required.

---

## Overview

Provides a complete payment processing API. Supports creating payments, manual status updates (PATCH), hard deletes, refunds, and webhook event handling. Payment records are stored in PostgreSQL with Redis caching for fast lookups.

The payment gateway is abstracted behind an interface with two implementations:
- **`MockPaymentGateway`** (default) — zero config, ideal for local dev and portfolio
- **`StripePaymentGateway`** — real Stripe integration, toggle via `PAYMENT_GATEWAY=stripe`

---

## Tech Stack

- **Spring Boot** 3.5.11
- **Spring Data JPA** — Database persistence
- **Spring Cloud OpenFeign** — Inter-service communication (Order Service)
- **PostgreSQL** — Payment data storage
- **Redis** — Distributed caching
- **Liquibase** — Database migrations
- **MapStruct** — DTO mapping
- **Stripe Java SDK** — Real payment processing (optional, toggled via config)
- **Spring Cloud Config** — Centralized configuration
- **Eureka Client** — Service registration
- **Springdoc OpenAPI** — API documentation

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 16+ (database: `ecomera_payment`)
- Redis 7+
- Config Server running on port 8888
- Eureka Server running on port 8761

> **No Stripe account needed** — the mock gateway works out of the box.

### Start the Service
```bash
mvn spring-boot:run
```

**Service available at:** `http://localhost:8085`

For real Stripe mode, set environment variables:
```bash
PAYMENT_GATEWAY=stripe STRIPE_SECRET_KEY=sk_test_... STRIPE_WEBHOOK_SECRET=whsec_... mvn spring-boot:run
```

---

## API Endpoints

### Payment Endpoints (requires `X-User-Id` header)

| Method | Endpoint | Description | Body / Params |
|--------|----------|-------------|---------------|
| POST | `/api/v1/payments` | Create a payment (amount sourced from Order Service) | `{ "orderId": "uuid", "paymentMethod": "CREDIT_CARD" }` |
| GET | `/api/v1/payments/{id}` | Get payment by ID | — |
| GET | `/api/v1/payments/order/{orderId}` | Get payment by order ID | — |

### Webhook (mock: any `Stripe-Signature` works)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments/webhook` | Ingest payment lifecycle events |

### Admin/Manager Endpoints (requires `ADMIN` or `MANAGER` in `X-User-Roles`)

| Method | Endpoint | Description | Body / Params |
|--------|----------|-------------|---------------|
| GET | `/api/v1/payments` | List all payments (paginated) | `?page=0&size=10&sortBy=createdAt&direction=desc` |
| PATCH | `/api/v1/payments/{id}` | Update payment status/method | `{ "status": "REFUNDED", "paymentMethod": "CREDIT_CARD" }` |
| POST | `/api/v1/payments/{id}/refund` | Refund a payment | `{ "amount": 10.00 }` (omit for full refund) |

### Admin-Only Endpoints (requires `ADMIN` in `X-User-Roles`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/api/v1/payments/{id}` | Hard-delete a payment (only if FAILED or PENDING) |

### Health & Docs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | OpenAPI documentation |

---

## Mock Mode (Default)

No external dependencies. The `MockPaymentGateway` simulates Stripe with zero config.

### Creating a payment
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-User-Roles: USER" \
  -H "Content-Type: application/json" \
  -d '{"orderId": "2cacce21-7ebe-42b0-915e-2bed184ef5f7", "paymentMethod": "CREDIT_CARD"}'
```

Response includes a `stripePaymentIntentId` like `pi_mock_<uuid>`. Use this ID to trigger webhooks.

### Simulating a webhook event
The mock webhook endpoint accepts any `Stripe-Signature` value. The body is a simple JSON with `type` and `payment_intent_id`:
```bash
curl -X POST http://localhost:8080/api/v1/payments/webhook \
  -H "Stripe-Signature: anything" \
  -H "Content-Type: application/json" \
  -d '{"type": "payment_intent.succeeded", "payment_intent_id": "pi_mock_abc-123"}'
```

Supported event types:
- `payment_intent.succeeded` — marks payment as SUCCEEDED, notifies Order Service via Feign
- `payment_intent.payment_failed` — marks payment as FAILED

### Switching to Stripe
Set `PAYMENT_GATEWAY=stripe` and provide valid `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET`.

---

## Database Schema

```
payment
├── id (UUID, PK)
├── order_id (UUID, NOT NULL, indexed)
├── user_id (UUID, NOT NULL)
├── stripe_payment_intent_id (VARCHAR, UNIQUE)
├── amount (DECIMAL, NOT NULL)
├── currency (VARCHAR, NOT NULL)
├── payment_method (VARCHAR, NOT NULL)   — PAYPAL / CREDIT_CARD / BANK_TRANSFER
├── status (VARCHAR, NOT NULL)           — PENDING / SUCCEEDED / FAILED / REFUNDED / PARTIALLY_REFUNDED
├── created_at (TIMESTAMP)
├── updated_at (TIMESTAMP)
├── created_by (VARCHAR)
└── updated_by (VARCHAR)
```

---

## Architecture

```
Client → API Gateway (port 8080)
              ↓
   Payment Service (port 8085)
      ↓          ↓           ↓
 PostgreSQL    Redis     Order Service (via Feign)
                           ↓
                     Config Server (configs)
                           ↓
                     Eureka Server (registration)
```

### Payment Flow (Mock Mode)
1. Client creates a payment via `POST /payments`
2. **Payment Service** fetches order total from **Order Service** (Feign), creates a mock intent
3. Client (or test script) sends a mock webhook event to `/payments/webhook` with the payment intent ID
4. **Payment Service** updates status to SUCCEEDED/FAILED and notifies **Order Service** via Feign
5. Order status is updated to CONFIRMED / CANCELLED accordingly

### Payment Flow (Stripe)
1. Client creates a PaymentIntent via `POST /payments`
2. Client confirms payment on frontend (Stripe Elements / Checkout)
3. Stripe sends a webhook event to `/api/v1/payments/webhook`
4. **Payment Service** verifies signature, updates payment status, and notifies **Order Service**

---

## Status Transition Rules

| Current | Target | Allowed? |
|---------|--------|----------|
| PENDING | SUCCEEDED / FAILED / REFUNDED | ✅ |
| SUCCEEDED | REFUNDED / PARTIALLY_REFUNDED | ✅ |
| FAILED | PENDING (reset for retry) | ✅ |
| REFUNDED | anything | ❌ (terminal) |

When status changes to SUCCEEDED → Order → CONFIRMED, FAILED/REFUNDED → Order → CANCELLED.

---

## Configuration

Configuration fetched from **Config Server** (`payment-service.yml`):

```yaml
payment:
  gateway: mock                        # or "stripe"
  default-currency: MAD

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecomera_payment
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8085

stripe:
  secret-key: ${STRIPE_SECRET_KEY}     # only needed when gateway=stripe
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
```

---

## Docker Support

### Build Image
```bash
docker build -t ecomera-payment-service .
```

### Run Container
```bash
docker run -p 8085:8085 \
  -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e EUREKA_SERVER_URL=http://eureka:8761/eureka/ \
  ecomera-payment-service
```

---

## Testing

```bash
# Unit tests
mvn test
```

### Manual test flow (mock mode)
1. Create payment → get `stripePaymentIntentId`
2. Send webhook `{"type":"payment_intent.succeeded","payment_intent_id":"pi_mock_<id>"}`
3. Verify payment status is SUCCEEDED via `GET /payments/{id}`
4. Test refund, PATCH, DELETE as needed

---

## Related Services

**Infrastructure:**
- [Config Server](https://github.com/ecomera-ecosystem/ecomera-config-server) — Centralized configuration
- [Eureka Server](https://github.com/ecomera-ecosystem/ecomera-eureka-service-registry) — Service discovery
- [API Gateway](https://github.com/ecomera-ecosystem/ecomera-api-gateway) — Entry point

**Business Services:**
- [Auth Service](https://github.com/ecomera-ecosystem/ecomera-auth-service) — Authentication & authorization
- [Order Service](https://github.com/ecomera-ecosystem/ecomera-order-service) — Order management (payment notifies via Feign)

---

## License

MIT License — see [LICENSE](LICENSE) file for details

---

**Status:** Active Development
