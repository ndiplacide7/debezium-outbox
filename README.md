# Debezium Outbox Pattern

A practical reference implementation of the **Transactional Outbox Pattern** using Debezium CDC, Apache Kafka, and Spring Boot 3.

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Services & Ports](#services--ports)
- [Register the Debezium Connector](#register-the-debezium-connector)
- [API Reference](#api-reference)
- [Kafka Topics & Events](#kafka-topics--events)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Overview

In a microservices architecture, reliably publishing an event to a message broker **in the same atomic step as a database write** is hard. You can't join a DB transaction with a Kafka producer — if the broker is down when you try to publish, or if the service crashes between the DB commit and the `send()` call, events get lost.

The **Transactional Outbox Pattern** solves this without a distributed transaction:

1. The service writes its business change **and** the event to the same database, in the same transaction.
2. A CDC tool (Debezium) reads the event rows from the outbox table by tailing the DB's write-ahead log (WAL).
3. Debezium publishes them to Kafka — guaranteed, even if the service was down when the transaction committed.

---

## How It Works

```
POST /orders
     │
     └─► @Transactional boundary
           ├─ INSERT INTO purchase_order …
           ├─ INSERT INTO order_line …
           ├─ INSERT INTO outboxevent (OrderCreated payload)
           └─ INSERT INTO outboxevent (InvoiceCreated payload)
                │
                │  Debezium reads WAL via pgoutput
                ▼
           Kafka Connect (EventRouter SMT)
                │
                ├──► topic: Order.events    ← ShipmentConsumer listens here
                └──► topic: Customer.events
```

The outbox table is **ephemeral** — Debezium deletes rows after publishing them (tombstone approach). The business tables and the event records always commit together or not at all.

---

## Architecture

| Component | Technology | Role |
|-----------|-----------|------|
| Order Service | Spring Boot 3.3, JPA | REST API, writes orders + outbox events |
| Shipment Service | Spring Boot 3.3, Kafka | Consumes `Order.events`, creates shipments |
| Order DB | PostgreSQL 16 (`wal_level=logical`) | Stores orders, order lines, outbox events |
| Shipment DB | PostgreSQL 16 | Stores shipments, consumed-message dedup table |
| Debezium | Kafka Connect 2.7 | CDC: tails WAL, routes events via EventRouter SMT |
| Kafka | Apache Kafka 3.8 (KRaft) | Event backbone — no Zookeeper |
| Kafka UI | provectuslabs/kafka-ui | Browse topics and messages in browser |

---

## Prerequisites

- **Docker & Docker Compose** v2+
- **Java 21** + **Maven 3.9+** (only needed for local development outside Docker)
- **curl** or **Postman** (to register the connector and test the API)

---

## Quick Start

### 1. Start the infrastructure

```bash
docker compose up -d
```

This starts Kafka, both PostgreSQL databases, Debezium Kafka Connect, and both Spring Boot services. Wait ~30 seconds for all health checks to pass.

```bash
docker compose ps   # all services should show "healthy" or "running"
```

### 2. Register the Debezium connector

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @register-connector.json
```

Verify it is running:

```bash
curl http://localhost:8083/connectors/outbox-connector/status
```

You should see `"state": "RUNNING"` for both the connector and its task.

### 3. Create an order

```bash
curl -X POST http://localhost:8085/orders \
  -H "Content-Type: application/json" \
  -d @requests/create-order.json
```

Within seconds, check **Kafka UI** at `http://localhost:8090` — you will see messages appear on the `Order.events` and `Customer.events` topics, and the Shipment Service will have created a shipment record in its own database.

---

## Services & Ports

| Service | URL / Port | Notes |
|---------|-----------|-------|
| Order Service REST API | `http://localhost:8085` | Exposed by Docker; `8080` when running locally |
| Order Service Swagger UI | `http://localhost:8080/swagger-ui.html` | Available when running locally via Maven |
| Order Service OpenAPI spec | `http://localhost:8080/v3/api-docs` | Machine-readable OpenAPI 3.0 JSON |
| Kafka UI | `http://localhost:8090` | Browse topics, messages, consumer groups |
| Debezium Kafka Connect API | `http://localhost:8083` | Manage connectors |
| Order DB (PostgreSQL) | `localhost:5433` | DB: `orderdb`, user: `postgresuser` |
| Shipment DB (PostgreSQL) | `localhost:5434` | DB: `shipmentdb`, user: `postgresuser` |
| Kafka broker | `localhost:9092` | Internal only; `kafka:9092` inside Docker network |

> **Local development:** Run the Order Service outside Docker with `mvn spring-boot:run` inside `order-service/`. The app connects to `localhost:5433` as configured in `application.properties`. Swagger UI is then available at `http://localhost:8080/swagger-ui.html`.

---

## Register the Debezium Connector

The connector configuration lives in `register-connector.json`. Key settings:

| Setting | Value | Why |
|---------|-------|-----|
| `connector.class` | `PostgresConnector` | Tails the PostgreSQL WAL |
| `plugin.name` | `pgoutput` | Native PostgreSQL logical decoding plugin |
| `schema.include.list` | `inventory` | Only watch this schema |
| `table.include.list` | `inventory.outboxevent` | Only watch the outbox table |
| `transforms` | `EventRouter` | Debezium SMT that routes messages to the correct topic based on `aggregatetype` |
| `route.topic.replacement` | `${routedByValue}.events` | `Order` → `Order.events`, `Customer` → `Customer.events` |

Register:
```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @register-connector.json
```

Check status:
```bash
curl http://localhost:8083/connectors/outbox-connector/status | jq
```

Delete and re-register (if you need to reset):
```bash
curl -X DELETE http://localhost:8083/connectors/outbox-connector
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d @register-connector.json
```

---

## API Reference

Base URL (local): `http://localhost:8080`
Base URL (Docker): `http://localhost:8085`

### `POST /orders` — Create an order

Creates a purchase order. All line items start with status `ENTERED`.
Writes two outbox events atomically: `OrderCreated` and `InvoiceCreated`.

**Request body**
```json
{
  "customerId": 42,
  "orderDate": "2026-05-14T10:30:00",
  "lineItems": [
    { "item": "Laptop",   "quantity": 1, "totalPrice": 1299.99 },
    { "item": "Mouse",    "quantity": 2, "totalPrice": 39.99   }
  ]
}
```

**Response `200 OK`**
```json
{
  "id": 1,
  "customerId": 42,
  "orderDate": "2026-05-14T10:30:00",
  "lineItems": [
    { "id": 1, "item": "Laptop", "quantity": 1, "totalPrice": 1299.99, "status": "ENTERED" },
    { "id": 2, "item": "Mouse",  "quantity": 2, "totalPrice": 39.99,   "status": "ENTERED" }
  ]
}
```

| Status | Condition |
|--------|-----------|
| `200` | Order created successfully |
| `400` | Missing/invalid fields (`customerId`, `orderDate`, or empty `lineItems`) |
| `415` | Missing `Content-Type: application/json` header |

---

### `PUT /orders/{orderId}/lines/{lineId}` — Update an order line

Updates the status of a single order line.
Writes one outbox event atomically: `OrderLineUpdated`.

Valid `newStatus` values: `ENTERED` · `CANCELLED` · `SHIPPED`

**Request body**
```json
{ "newStatus": "CANCELLED" }
```

**Response `200 OK`** — full order snapshot (same shape as create response)

| Status | Condition |
|--------|-----------|
| `200` | Line updated successfully |
| `400` | Missing `newStatus` or unrecognised status value |
| `404` | Order ID or line ID does not exist |

---

## Kafka Topics & Events

| Topic | Event type | Produced by | Consumed by |
|-------|-----------|-------------|-------------|
| `Order.events` | `OrderCreated` | Order Service | Shipment Service |
| `Order.events` | `OrderLineUpdated` | Order Service | Shipment Service (logged) |
| `Customer.events` | `InvoiceCreated` | Order Service | *(extend as needed)* |

Events are routed by the Debezium **EventRouter SMT**, which reads the `aggregatetype` column from the outbox table and maps it to `{aggregatetype}.events`.

The Shipment Service uses an **idempotency table** (`consumed_message`) to safely handle Kafka's at-least-once delivery — duplicate event IDs are detected and skipped.

---

## Testing

### Swagger UI (interactive)

Start the Order Service locally, then open:

```
http://localhost:8080/swagger-ui.html
```

Every endpoint, request schema, and response is documented and executable directly in the browser.

### Postman collection

Import `postman/order-service.postman_collection.json` into Postman (**File → Import**).

The collection contains **16 test scenarios** with automated assertions:

| Folder | Scenarios |
|--------|----------|
| `POST /orders` | 3 happy paths · 5 error paths |
| `PUT /orders/{orderId}/lines/{lineId}` | 3 happy paths · 5 error paths |

**Usage tip:** Run any "Create Order" request first. Its test script automatically stores the returned `id` and `lineItems[0].id` as collection variables (`orderId`, `lineId`), so all update scenarios are pre-wired and ready to run.

### Manual curl examples

```bash
# Create an order
curl -X POST http://localhost:8085/orders \
  -H "Content-Type: application/json" \
  -d @requests/create-order.json

# Cancel order line 1 on order 1
curl -X PUT http://localhost:8085/orders/1/lines/1 \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"CANCELLED"}'

# Cancel an order line (from sample file)
curl -X PUT http://localhost:8085/orders/1/lines/1 \
  -H "Content-Type: application/json" \
  -d @requests/cancel-order-line.json
```

---

## Project Structure

```
debezium-outbox/
├── docker-compose.yml              # Full stack: Kafka, DBs, Debezium, services
├── register-connector.json         # Debezium PostgreSQL connector config
├── requests/                       # Sample HTTP request bodies
│   ├── create-order.json
│   └── cancel-order-line.json
├── postman/
│   └── order-service.postman_collection.json
│
├── order-service/                  # Spring Boot — REST API + outbox writer
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/orderservice/
│       ├── config/         OpenApiConfig.java
│       ├── event/          OrderCreatedEvent, OrderLineUpdatedEvent, InvoiceCreatedEvent
│       ├── exception/      EntityNotFoundException
│       ├── model/          PurchaseOrder, OrderLine, OrderLineStatus
│       ├── outbox/         Outbox, OutboxRepository, ExportedEvent
│       ├── repository/     PurchaseOrderRepository
│       ├── rest/           OrderController, DTOs, GlobalExceptionHandler
│       └── service/        OrderService
│
└── shipment-service/               # Spring Boot — Kafka consumer
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/shipmentservice/
        ├── consumer/       ShipmentConsumer (idempotent)
        ├── model/          Shipment, ConsumedMessage
        └── repository/     ShipmentRepository, ConsumedMessageRepository
```
