package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLineStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderLineRequest {

    @NotNull
    private OrderLineStatus newStatus;

    public OrderLineStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderLineStatus newStatus) { this.newStatus = newStatus; }
}