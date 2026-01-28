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
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
```
### Stop (MySQL)
 ```bash
 docker compose -f infra/docker-compose.yml down
 ```

### Reset (DANGEROUS: deletes volumes)
```bash
docker compose -f infra/docker-compose.yml down -v
```

### Start services (Each service)

```
cd auth
mvn spring-boot:run
```