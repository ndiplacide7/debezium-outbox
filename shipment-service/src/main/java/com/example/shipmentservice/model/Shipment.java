package com.example.shipmentservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a shipment created when the shipment service processes
 * an OrderCreated event from the Order service.
 *
 * Note: this is a completely separate database from the order service.
 * The shipment service only knows about orders via Kafka events —
 * never via a direct DB join or REST call.
 */
@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long customerId;

    private long orderId;

    private LocalDateTime orderDate;

    public Long getId() { return id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
}