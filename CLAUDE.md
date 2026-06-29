# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Principles to Follow
- Always write clean code following SOLID, Clean Code, and Design Patterns best practices.
- After each implementation, explain the problem and which solution was used.

# Testing Rules (Mandatory)
- After **any** change to a service's source code, run `mvn test` inside that service's directory and ensure all tests pass before reporting the task as done.
- If a test fails after a change, fix both the code and the test before finishing.
- Every new method must have at minimum one success test and one failure test. Add extra scenarios where business logic warrants it.
- Test method naming convention: `methodName_shouldDescription_whenCondition`.
- The `*ApplicationTests.contextLoads()` classes are integration tests marked `@Disabled` — do not enable them in unit test runs.

## Unit Tests
- Use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), no Spring context — keep them fast and isolated.

## Integration Tests
- Controllers: use `@WebMvcTest(XxxController.class)` + `@Import({SecurityConfig.class, XxxIT.MethodSecurityConfig.class})`.
  - Add a nested `@TestConfiguration @EnableMethodSecurity static class MethodSecurityConfig {}` so `@PreAuthorize` is enforced.
  - Inject mock JWT via `SecurityMockMvcRequestPostProcessors.jwt().authorities(new SimpleGrantedAuthority("ROLE_X"))`.
  - Always disable Spring Cloud Config and Eureka: `spring.cloud.config.enabled=false`, `eureka.client.enabled=false`.
  - Provide `security.jwt.secret-key=test-secret-key-for-integration-test-32ch` when `NimbusJwtDecoder` is wired.
- Event consumers (no controllers): use `@SpringBootTest(webEnvironment = WebEnvironment.MOCK)` + `@MockBean` for external ports.
  - Disable SQS polling: `spring.cloud.aws.sqs.enabled=false`.
  - Services with JPA: add H2 to test scope in pom.xml and disable Flyway: `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=create-drop`.
  - Provide all `@Value`-injected properties referenced by beans loaded in context (e.g. SQS queue names, S3 bucket, AWS credentials).

# Commit Standards
- Use commit messages in English with the following structure: `"feat(module-name): summary. detailed description"`
- Never include a signature in commits.

## Build & Run Commands

Each service is built independently with Maven. There is no root `pom.xml`.

```bash
# Build a single service (skip tests)
cd <service-name>
mvn clean package -DskipTests

# Run a single service
mvn spring-boot:run

# Run tests for a single service
mvn test

# Run a single test class
mvn test -Dtest=MyServiceTest

# Start all infrastructure (LocalStack + PostgreSQL databases + PgAdmin)
docker compose up -d

# Initialize AWS resources on LocalStack (SNS topics, SQS queues, S3 bucket)
# This runs automatically via the init-aws.sh hook mounted into the LocalStack container
```

Start-up order matters: `eureka-service` → `config-service` → remaining services. The API gateway resolves service names via Eureka.

## Architecture Overview

This is a Spring Boot 3.5 / Java 21 microservices system that implements a subscription billing engine. All services register with **Eureka** and pull configuration from a **Spring Cloud Config Server** (backed by a separate Git repo: `https://github.com/CaiquePirs/billing-engine-config-service`).

### Services

| Service | Port | Responsibility |
|---|---|---|
| `eureka-service` | 8761 | Service registry (Netflix Eureka) |
| `config-service` | 8888 | Centralized config server (Git-backed) |
| `api-gateway` | — | Spring Cloud Gateway; routes all traffic to downstream services |
| `authentication-service` | — | JWT issuance (HMAC256), user registration, internal service tokens |
| `customers-service` | — | Customer CRUD, Stripe customer creation |
| `subscription-service` | — | Plans CRUD, subscription lifecycle, Stripe subscription creation |
| `payment-service` | — | Stripe webhook handling, payment processing via Stripe |
| `invoice-service` | — | PDF invoice generation (openhtmltopdf), S3 storage |
| `notification-service` | — | Email delivery via AWS SES using HTML templates |

### Event-Driven Flow (AWS SNS → SQS Fan-out)

All async communication uses **AWS SNS fan-out to SQS**. The `init-aws.sh` script creates these resources in LocalStack on startup.

```
subscription-created-topic (SNS)
  ├── process-payment-queue         → payment-service
  └── notify-new-subscription-queue → notification-service

payment-approved-topic (SNS)
  ├── notify-payment-approved-queue → notification-service
  ├── generate-invoice-queue        → invoice-service
  └── active-subscription-queue    → subscription-service

payment-failed-topic (SNS)
  ├── notify-payment-failed-queue   → notification-service
  └── desactivate-subscription-queue → subscription-service
```

Consumers use `@SqsListener` from `io.awspring.cloud:spring-cloud-aws-starter-sqs`. Producers use `SnsClient` (AWS SDK v2) and serialize events to JSON via `ObjectMapper` before publishing.

Inbound SQS messages from SNS are wrapped in a `SnsMessage` record — deserialize the `.Message()` field to get the actual event payload.

### Inter-Service HTTP Communication

Services that need to call other services use **OpenFeign** clients. Service-to-service requests are authenticated using internal JWT tokens: a `FeignInterceptorConfig` (implements `RequestInterceptor`) automatically obtains a short-lived internal token from `authentication-service` before each outbound Feign call, using `SUBSCRIPTION_SERVICE_CLIENT_ID` and `SUBSCRIPTION_SERVICE_SECRET` env vars.

### Security

- All services except `authentication-service` are stateless OAuth2 resource servers that validate HMAC256 JWTs via `NimbusJwtDecoder`.
- The shared `SECRET_KEY` env var is used by all services to validate tokens (set in the config-service Git repo).
- JWT claims include `role`, `customer_id` (user tokens) or `scope`, `client_id` (internal service tokens). Tokens expire in 15 min (user) / 30 min (internal).

### Database

Each service has its own **PostgreSQL** database (isolated per service). Schema migrations use **Flyway** (`db/migration/V{n}__*.sql`). Databases run in Docker on different host ports:

| DB | Host Port |
|---|---|
| customers-postgres | 5432 |
| authentication-postgres | 5433 |
| subscriptions-postgres | 5434 |
| payments-postgres | 5435 |
| invoices-postgres | 5436 |

### Stripe Integration

`subscription-service`, `payment-service`, and `customers-service` integrate with Stripe. The subscription service creates Stripe customers and products/prices. The payment service receives Stripe webhook events at `/api/v1/webhooks/stripe/**` and publishes SNS events based on payment outcome.

### Common Internal Patterns

- **Mappers** — each service has a `mapper/` package with manual mapping classes (no MapStruct). Keep domain model separate from DTOs.
- **Events data package** — `events/data/` holds event POJOs (records or classes) shared between producers and consumers within a service.
- **Global exception handlers** — `@RestControllerAdvice` in `controller/advice/handler/GlobalExceptionHandler` returns structured `ErrorResponseDTO`.
- **AuditLog / AuditEntity** — base classes for `createdAt`/`updatedAt` audit fields, used across all services.
- **Strategy pattern** — `notification-service` uses a port/adapter structure (`SendNotificationPort` / `SendNotificationAdapter`) for email delivery.