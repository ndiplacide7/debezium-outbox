package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to update an order line status")
public class UpdateOrderLineRequest {

    @Schema(description = "New status for the order line", example = "CANCELLED",
            allowableValues = {"ENTERED", "CANCELLED", "SHIPPED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private OrderLineStatus newStatus;

    public OrderLineStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderLineStatus newStatus) { this.newStatus = newStatus; }
}
