package com.example.orderservice.event;

import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.outbox.ExportedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired when an order is placed — represents a billing event.
 *
 * Notice: aggregateType = "Customer" (not "Order").
 * This routes to a DIFFERENT Kafka topic: "Customer.events".
 * The aggregateId is the customer ID.
 *
 * This demonstrates that a single business action (place order) can
 * produce events for MULTIPLE aggregates / topics in one transaction.
 */
public class InvoiceCreatedEvent implements ExportedEvent {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String AGGREGATE_TYPE = "Customer";

    private final PurchaseOrder order;
    private final Instant timestamp;

    private InvoiceCreatedEvent(PurchaseOrder order) {
        this.order = order;
        this.timestamp = Instant.now();
    }

    public static InvoiceCreatedEvent of(PurchaseOrder order) {
        return new InvoiceCreatedEvent(order);
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getAggregateId() {
        // Key on customer ID — events for the same customer go to the same partition
        return String.valueOf(order.getCustomerId());
    }

    @Override
    public String getType() {
        return "InvoiceCreated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        BigDecimal invoiceValue = order.getLineItems().stream()
                .map(OrderLine::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("orderId", order.getId());
        payload.put("invoiceDate", order.getOrderDate().toString());
        payload.put("invoiceValue", invoiceValue);
        return payload;
    }
}