package com.example.orderservice.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * The Outbox table.
 *
 * Debezium watches this table via PostgreSQL logical replication (WAL).
 * Every INSERT here becomes a Kafka message — atomically with the business
 * transaction that triggered it, because they share the same DB transaction.
 *
 * Column naming matters: the EventRouter SMT uses these exact names by default:
 *   id            → event deduplication key
 *   aggregatetype → Kafka topic routing (maps to route.by.field)
 *   aggregateid   → Kafka message key
 *   type          → placed into eventType header
 *   payload       → becomes the Kafka message value
 *   timestamp     → becomes the Kafka message timestamp
 */
@Entity
@Table(name = "outboxevent")
public class Outbox {

    @Id
    private UUID id;

    @Column(name = "aggregatetype", nullable = false, length = 75)
    private String aggregateType;

    @Column(name = "aggregateid", nullable = false, length = 50)
    private String aggregateId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    // jsonb: Debezium reads this as a JSON document and forwards it as a JSON
    // object to Kafka (not as a quoted string, which is what text columns produce)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
