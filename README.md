# Flash Sale Microservices (MVP)

A distributed microservices system for high-concurrency flash sale scenarios.

## Architecture (MVP)

- gatewayserver: API Gateway (routing, security, rate limiting)
- auth: authentication & JWT issuing
- catalog: event & ticket read model
- inventory: stock management (source of truth)
- order: order lifecycle & idempotency

Each service is an independent Spring Boot application with its own database.

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Spring Cloud 2023.x
- MySQL 8 (Docker)
- Maven (multi-module parent)

## Local Development

### Start infrastructure (MySQL)

```bash
docker compose up -d
```

### Start services
Each service can be started independently:

```
cd auth
mvn spring-boot:run
```