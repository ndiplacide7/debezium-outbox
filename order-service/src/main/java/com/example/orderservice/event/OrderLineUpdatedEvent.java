package com.example.orderservice.event;

import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.OrderLineStatus;
import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.outbox.ExportedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

/**
 * Fired when an order line changes status (e.g. ENTERED → CANCELLED).
 * aggregateType = "Order"  →  same topic "Order.events" as OrderCreatedEvent
 * aggregateId   = order ID →  same partition as the parent order (ordering guarantee)
 */
public class OrderLineUpdatedEvent implements ExportedEvent {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String AGGREGATE_TYPE = "Order";

    private final PurchaseOrder order;
    private final OrderLine orderLine;
    private final OrderLineStatus oldStatus;
    private final Instant timestamp;

    private OrderLineUpdatedEvent(PurchaseOrder order, OrderLine orderLine, OrderLineStatus oldStatus) {
        this.order = order;
        this.orderLine = orderLine;
        this.oldStatus = oldStatus;
        this.timestamp = Instant.now();
    }

    public static OrderLineUpdatedEvent of(PurchaseOrder order, OrderLine line, OrderLineStatus oldStatus) {
        return new OrderLineUpdatedEvent(order, line, oldStatus);
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(order.getId());
    }

    @Override
    public String getType() {
        return "OrderLineUpdated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("orderId", order.getId());
        payload.put("orderLineId", orderLine.getId());
        payload.put("oldStatus", oldStatus.name());
        payload.put("newStatus", orderLine.getStatus().name());
        return payload;
    }
}