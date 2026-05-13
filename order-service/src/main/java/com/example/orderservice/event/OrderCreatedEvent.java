package com.example.orderservice.event;

import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.outbox.ExportedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

/**
 * Fired when a new order is placed.
 * aggregateType = "Order"  →  routed to Kafka topic "Order.events"
 * aggregateId   = order ID →  used as Kafka message key (ordering per order)
 */
public class OrderCreatedEvent implements ExportedEvent {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String AGGREGATE_TYPE = "Order";

    private final PurchaseOrder order;
    private final Instant timestamp;

    private OrderCreatedEvent(PurchaseOrder order) {
        this.order = order;
        this.timestamp = Instant.now();
    }

    public static OrderCreatedEvent of(PurchaseOrder order) {
        return new OrderCreatedEvent(order);
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
        return "OrderCreated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", order.getId());
        payload.put("customerId", order.getCustomerId());
        payload.put("orderDate", order.getOrderDate().toString());

        ArrayNode lines = payload.putArray("lineItems");
        for (OrderLine line : order.getLineItems()) {
            ObjectNode lineNode = mapper.createObjectNode();
            lineNode.put("id", line.getId());
            lineNode.put("item", line.getItem());
            lineNode.put("quantity", line.getQuantity());
            lineNode.put("totalPrice", line.getTotalPrice());
            lineNode.put("status", line.getStatus().name());
            lines.add(lineNode);
        }

        return payload;
    }
}
