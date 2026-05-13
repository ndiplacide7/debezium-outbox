package com.example.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_order_line")
    @SequenceGenerator(name = "seq_order_line", sequenceName = "seq_order_line", allocationSize = 50)
    private Long id;

    private String item;

    private int quantity;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderLineStatus status;

    // Many order lines belong to one purchase order
    // JsonIgnore prevents infinite recursion when serialising
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    public Long getId() { return id; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public OrderLineStatus getStatus() { return status; }
    public void setStatus(OrderLineStatus status) { this.status = status; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
}
