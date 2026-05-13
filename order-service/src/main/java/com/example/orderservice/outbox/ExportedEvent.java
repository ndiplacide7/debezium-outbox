package com.example.orderservice.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Contract every domain event must satisfy to be stored in the Outbox table.
 *
 * aggregateType  → Kafka topic prefix  (e.g. "Order"    → "Order.events")
 * aggregateId    → Kafka message key   (e.g. "42"       → ordering guarantee per order)
 * type           → Kafka header        (e.g. "OrderCreated")
 * payload        → Kafka message value (full event JSON)
 */
public interface ExportedEvent {

    String getAggregateType();

    String getAggregateId();

    String getType();

    Instant getTimestamp();

    JsonNode getPayload();
}
