package com.example.orderservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_purchase_order")
    @SequenceGenerator(name = "seq_purchase_order", sequenceName = "seq_purchase_order", allocationSize = 50)
    private Long id;

    private long customerId;

    private LocalDateTime orderDate;

    // One order has many lines; cascade means lines are saved with the order
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lineItems = new ArrayList<>();

    public Long getId() { return id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderLine> getLineItems() { return lineItems; }
    public void setLineItems(List<OrderLine> lineItems) { this.lineItems = lineItems; }
}
