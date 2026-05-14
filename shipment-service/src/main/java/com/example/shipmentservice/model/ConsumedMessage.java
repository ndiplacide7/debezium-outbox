package com.example.shipmentservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency log — tracks every Kafka event ID that has been processed.
 *
 * Problem: Kafka guarantees at-least-once delivery. If the consumer crashes
 * AFTER writing to the DB but BEFORE committing the Kafka offset, Kafka
 * will redeliver the message on restart.
 *
 * Solution: before processing any event, check if its UUID is already here.
 * If yes → skip. If no → process and then record it here.
 *
 * The check + insert happens in the same @Transactional as the Shipment insert,
 * so they are atomic. No race conditions.
 */
@Entity
@Table(name = "consumed_message")
public class ConsumedMessage {

    @Id
    private UUID eventId;

    private Instant timeOfReceiving;

    public ConsumedMessage() {}

    public ConsumedMessage(UUID eventId, Instant timeOfReceiving) {
        this.eventId = eventId;
        this.timeOfReceiving = timeOfReceiving;
    }

    public UUID getEventId() { return eventId; }
    public Instant getTimeOfReceiving() { return timeOfReceiving; }
}