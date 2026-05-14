package com.example.orderservice.service;

import com.example.orderservice.event.InvoiceCreatedEvent;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderLineUpdatedEvent;
import com.example.orderservice.exception.EntityNotFoundException;
import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.OrderLineStatus;
import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.outbox.ExportedEvent;
import com.example.orderservice.outbox.Outbox;
import com.example.orderservice.outbox.OutboxRepository;
import com.example.orderservice.repository.PurchaseOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final PurchaseOrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(PurchaseOrderRepository orderRepository,
                        OutboxRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an order and records two events in the same DB transaction:
     *   1. OrderCreatedEvent  → routes to "Order.events"
     *   2. InvoiceCreatedEvent → routes to "Customer.events"
     *
     * Because both writes happen in the same @Transactional boundary,
     * either BOTH land in the DB (and Debezium picks them up) or NEITHER does.
     * This is the guarantee the Outbox Pattern provides.
     */
    @Transactional
    public PurchaseOrder addOrder(PurchaseOrder order) {
        // Wire up back-references so Hibernate persists the FK on each line
        order.getLineItems().forEach(line -> {
            line.setPurchaseOrder(order);
            line.setStatus(OrderLineStatus.ENTERED);
        });

        PurchaseOrder saved = orderRepository.save(order);

        // Persist events to the outbox table — same transaction, same commit
        saveToOutbox(OrderCreatedEvent.of(saved));
        saveToOutbox(InvoiceCreatedEvent.of(saved));

        return saved;
    }

    /**
     * Updates an order line's status and records the change event.
     * Again: business change + event = one transaction = guaranteed delivery.
     */
    @Transactional
    public PurchaseOrder updateOrderLine(long orderId, long lineId, OrderLineStatus newStatus) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        OrderLine line = order.getLineItems().stream()
                .filter(l -> l.getId() == lineId)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Order line not found: " + lineId));

        OrderLineStatus oldStatus = line.getStatus();
        line.setStatus(newStatus);

        saveToOutbox(OrderLineUpdatedEvent.of(order, line, oldStatus));

        return order;
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private void saveToOutbox(ExportedEvent event) {
        Outbox outbox = new Outbox();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType(event.getAggregateType());
        outbox.setAggregateId(event.getAggregateId());
        outbox.setType(event.getType());
        outbox.setTimestamp(event.getTimestamp());
        outbox.setPayload(toJson(event.getPayload()));
        outboxRepository.save(outbox);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}