# 💳 Subscription Billing Engine — Microservices

> 🚧 **Work in progress.** This is a personal learning project, built and evolved incrementally — some services/features described below are still partial or being actively reworked.

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

## 🛠️ Tech Stack

- Java 21 / Spring Boot 3.5
- Spring Cloud (Eureka, Config Server, Gateway, OpenFeign)
- PostgreSQL (one isolated database per service) + Flyway migrations
- AWS SNS/SQS/S3/SES (via LocalStack for local development)
- Stripe API
- Micrometer + OpenTelemetry Collector + Prometheus
- Docker Compose for local infrastructure