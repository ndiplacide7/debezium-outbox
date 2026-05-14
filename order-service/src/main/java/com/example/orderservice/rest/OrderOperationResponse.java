package com.example.orderservice.rest;

import com.example.orderservice.model.PurchaseOrder;

import java.time.LocalDateTime;
import java.util.List;

public class OrderOperationResponse {

    private Long id;
    private long customerId;
    private LocalDateTime orderDate;
    private List<OrderLineDto> lineItems;

    public static OrderOperationResponse from(PurchaseOrder order) {
        OrderOperationResponse r = new OrderOperationResponse();
        r.id = order.getId();
        r.customerId = order.getCustomerId();
        r.orderDate = order.getOrderDate();
        r.lineItems = order.getLineItems().stream().map(OrderLineDto::from).toList();
        return r;
    }

    public Long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public List<OrderLineDto> getLineItems() { return lineItems; }
}