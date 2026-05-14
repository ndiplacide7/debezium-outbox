package com.example.shipmentservice.consumer;

import com.example.shipmentservice.model.ConsumedMessage;
import com.example.shipmentservice.model.Shipment;
import com.example.shipmentservice.repository.ConsumedMessageRepository;
import com.example.shipmentservice.repository.ShipmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ShipmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentConsumer.class);

    private final ShipmentRepository shipmentRepository;
    private final ConsumedMessageRepository consumedMessageRepository;
    private final ObjectMapper objectMapper;

    public ShipmentConsumer(ShipmentRepository shipmentRepository,
                            ConsumedMessageRepository consumedMessageRepository,
                            ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.consumedMessageRepository = consumedMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens to the "Order.events" Kafka topic.
     *
     * Spring Kafka calls this method for every message.
     * We use ConsumerRecord (not just the value String) so we can read the
     * Kafka headers that Debezium's EventRouter placed there:
     *   - "id"        → the UUID of the Outbox record (for idempotency)
     *   - "eventType" → "OrderCreated" | "OrderLineUpdated" (for routing)
     *
     * @Transactional: the DB writes (Shipment + ConsumedMessage) happen in
     * one transaction. If either fails, both roll back and Kafka can redeliver.
     *
     * Acknowledgment (manual ack): we only tell Kafka "done, advance the offset"
     * AFTER the DB transaction commits successfully.
     */
    @KafkaListener(topics = "${kafka.topic.order-events}", groupId = "shipment-service")
    @Transactional
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventType = extractHeader(record, "eventType");
        String eventIdStr = extractHeader(record, "id");

        log.debug("Received Kafka message | topic={} partition={} offset={} eventType={} id={}",
                record.topic(), record.partition(), record.offset(), eventType, eventIdStr);

        // ── Idempotency check ──────────────────────────────────────────────
        // Kafka guarantees at-least-once delivery, so duplicates are possible.
        // The ConsumedMessage table is our deduplication guard.
        UUID eventId = UUID.fromString(eventIdStr);
        if (consumedMessageRepository.existsById(eventId)) {
            log.info("Event {} already processed — skipping duplicate", eventId);
            ack.acknowledge();
            return;
        }

        // ── Route by event type ────────────────────────────────────────────
        if ("OrderCreated".equals(eventType)) {
            handleOrderCreated(record.value());
        } else if ("OrderLineUpdated".equals(eventType)) {
            handleOrderLineUpdated(record.value());
        } else {
            log.warn("Unknown eventType '{}' — ignoring", eventType);
        }

        // ── Mark as processed (idempotency record) ─────────────────────────
        consumedMessageRepository.save(new ConsumedMessage(eventId, Instant.now()));

        // ── Commit Kafka offset only after DB transaction succeeds ─────────
        ack.acknowledge();
    }

    // ── Event Handlers ──────────────────────────────────────────────────────

    private void handleOrderCreated(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            // When the outbox payload column is text (not jsonb), Debezium wraps
            // the content in a JSON string — we unwrap it here defensively.
            JsonNode json = root.isTextual() ? objectMapper.readTree(root.asText()) : root;

            Shipment shipment = new Shipment();
            shipment.setOrderId(json.get("id").asLong());
            shipment.setCustomerId(json.get("customerId").asLong());
            shipment.setOrderDate(LocalDateTime.parse(json.get("orderDate").asText()));

            shipmentRepository.save(shipment);

            log.info("Created shipment for order {} (customer {})",
                    shipment.getOrderId(), shipment.getCustomerId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process OrderCreated event", e);
        }
    }

    private void handleOrderLineUpdated(String payload) {
        // In this demo we just log it — in a real service you might update
        // the shipment status based on the order line status change.
        log.info("OrderLineUpdated event received: {}", payload);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) {
            throw new IllegalArgumentException("Missing required Kafka header: " + headerName);
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
