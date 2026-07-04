# 💳 Subscription Billing Engine — Microservices

> 🚧 **Work in progress.** This is a personal learning project, built and evolved incrementally — some services/features described below are still partial or being actively reworked. See [Project Status](#-project-status) before assuming everything is production-ready.

This project simulates the real-world workflow of a subscription billing engine using a microservices architecture. It's being built to deepen my hands-on knowledge of **AWS**, **distributed systems design**, **resilience patterns**, and **microservices** in general — using Spring Boot, event-driven communication, and Stripe as the payment provider.

## 🏗️ Architecture of Success flow of the billing engine
![architecture-flow-billing-engine.png](architecture-flow-billing-engine.png)

## 🧩 Services
| Service | Responsibility |
|---|---|
| `eureka-service` | Service registry (Netflix Eureka) |
| `config-service` | Centralized configuration server (Git-backed) |
| `api-gateway` | Routes all incoming traffic to downstream services (Spring Cloud Gateway) |
| `authentication-service` | JWT issuance (HMAC256), user registration, internal service-to-service tokens |
| `customers-service` | Customer CRUD, Stripe customer creation |
| `subscription-service` | Plans CRUD, subscription lifecycle, Stripe subscription creation |
| `payment-service` | Stripe webhook handling and payment processing |
| `invoice-service` | PDF invoice generation and storage in S3 |
| `notification-service` | Transactional emails via AWS SES |

All services register with **Eureka** and pull their configuration from a **Spring Cloud Config Server**, backed by a separate Git repository.

## 🔄 Event-driven communication (AWS SNS → SQS fan-out)

Asynchronous communication between services is done via **AWS SNS topics fanning out to SQS queues**, provisioned locally through LocalStack:

```
subscription-created-topic (SNS)
  ├── process-payment-queue         → payment-service
  └── notify-new-subscription-queue → notification-service

payment-approved-topic (SNS)
  ├── notify-payment-approved-queue → notification-service
  ├── generate-invoice-queue        → invoice-service
  └── active-subscription-queue     → subscription-service

payment-failed-topic (SNS)
  ├── notify-payment-failed-queue    → notification-service
  └── desactivate-subscription-queue → subscription-service
```

Consumers use `@SqsListener`; producers publish JSON-serialized events through `SnsClient` (AWS SDK v2).

## 🔐 Security

- All services except `authentication-service` are stateless OAuth2 resource servers, validating HMAC256 JWTs.
- Service-to-service calls (via OpenFeign) are authenticated with short-lived internal JWTs.
- User tokens expire in 15 minutes; internal service tokens in 30 minutes.

## 💰 Payments

`subscription-service`, `payment-service`, and `customers-service` integrate with **Stripe**: subscription creation, products/prices, and webhook-driven payment outcomes.

## 📊 Observability

A metrics pipeline (Micrometer → OpenTelemetry Collector → Prometheus) is being rolled out service by service.

**Status:** `authentication-service`, `subscription-service`, `payment-service`, `invoice-service`, and `notification-service` are instrumented with custom business metrics. `customers-service` (and the infrastructure services) are still pending. Logs (Loki) and traces (Tempo) are planned but not implemented yet — see `OBSERVABILITY-PLAN.md` for the full rollout notes and debugging guide.

## 🛠️ Tech Stack

- Java 21 / Spring Boot 3.5
- Spring Cloud (Eureka, Config Server, Gateway, OpenFeign)
- PostgreSQL (one isolated database per service) + Flyway migrations
- AWS SNS/SQS/S3/SES (via LocalStack for local development)
- Stripe API
- Micrometer + OpenTelemetry Collector + Prometheus
- Docker Compose for local infrastructure

## ▶️ Running locally

```bash
# 1. Start infrastructure (LocalStack, PostgreSQL databases, PgAdmin, Prometheus, OTel Collector)
docker compose up -d

# 2. Start services in order (each in its own terminal)
cd eureka-service && mvn spring-boot:run
cd config-service && mvn spring-boot:run
# then the remaining services, in any order

# 3. Run tests for a single service
cd <service-name> && mvn test
```

> ⚠️ Startup order matters: `eureka-service` → `config-service` → everything else. The API gateway resolves service names through Eureka.

## 📌 Project Status

This project is under active, incremental development. Expect:
- Observability coverage still expanding (`customers-service` not instrumented yet).
- Ongoing rework of business-metric naming/instrumentation across services.
- Logs and distributed tracing not implemented yet (planned next phases).
- Architecture diagrams that may lag slightly behind the latest code changes.

Contributions, suggestions, and issue reports are welcome — but keep in mind this is primarily a learning/portfolio project, not a maintained production system.
