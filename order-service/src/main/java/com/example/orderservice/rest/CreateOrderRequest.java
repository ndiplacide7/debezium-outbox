package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLine;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class CreateOrderRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private LocalDateTime orderDate;

    @NotEmpty
    private List<OrderLine> lineItems;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderLine> getLineItems() { return lineItems; }
    public void setLineItems(List<OrderLine> lineItems) { this.lineItems = lineItems; }
}