# Flash Sale Microservices (MVP)

A microservices-based flash sale system focusing on **high concurrency correctness**, **clear service boundaries**, and **evolution toward event-driven architecture**.

The project emphasizes **inventory correctness first** (no overselling, idempotency, explicit state machines) while remaining runnable as an MVP.

---

## Architecture

The system is composed of independent Spring Boot services with database-per-service isolation.

- **gatewayserver**  
  Unified entry point. Handles routing, JWT validation, and basic rate limiting.

- **auth**  
  User authentication service. Responsible for registration, login, and JWT issuing.

- **catalog**  
  Read-only service for events and tickets. Optimized for read traffic and backed by Redis cache.

- **inventory**  
  Inventory source of truth. Guarantees correctness under high concurrency.

- **order** *(in progress)*  
  Order lifecycle management and idempotent order creation.

---

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Spring Cloud 2023.x
- MySQL 8 (Docker)
- Redis (Catalog cache)
- Maven (multi-module)

---

## Implemented Services

### Gateway
- Path-based routing to backend services
- JWT decoding and validation
- Global authentication enforcement
- Simple IP-based rate limiting

### Catalog
- Event and ticket data model
- Public query APIs for events and tickets
- Redis cache-aside strategy for hot reads
- Read-heavy, write-light design
- Inventory data is eventually consistent

### Inventory (M4 Complete)

The inventory service is the **correctness core** of the system.

**Design guarantees**
- No overselling
- Idempotent APIs
- Explicit reservation state machine
- Inventory conservation at all times

**Data model**
- `inventory`: available / reserved / sold per ticket
- `inventory_reservations`: reservation-level idempotency and audit

**APIs**
- Initialize stock (admin-only)
- Reserve stock (idempotent, concurrency-safe)
- Commit reservation (reserved → sold)
- Release reservation (reserved → available)

**Concurrency strategy**
- Single-row atomic SQL updates with conditional checks
- No distributed locks
- Database is the single source of truth

**Reservation state machine**
INIT
├─ RESERVED ──┬─ COMMITTED
│ └─ RELEASED
└─ FAILED

Illegal state transitions are explicitly rejected.

---

## Local Development

### Start Infrastructure
```bash
docker compose -f infra/docker-compose.yml up -d
```

### Stop Infrastructure
```bash
docker compose -f infra/docker-compose.yml down
```

### Reset (⚠️ deletes data)
```bash
docker compose -f infra/docker-compose.yml down -v
```

### Run Services
Each service runs independently:
```bash
cd gatewayserver && mvn spring-boot:run
cd auth          && mvn spring-boot:run
cd catalog       && mvn spring-boot:run
cd inventory     && mvn spring-boot:run
```