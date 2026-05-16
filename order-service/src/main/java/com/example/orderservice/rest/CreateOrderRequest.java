package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Payload to create a new purchase order")
public class CreateOrderRequest {

    @Schema(description = "ID of the customer placing the order", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long customerId;

    @Schema(description = "Date and time the order was placed (ISO-8601)", example = "2026-05-14T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LocalDateTime orderDate;

    @Schema(description = "At least one order line is required", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<OrderLine> lineItems;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderLine> getLineItems() { return lineItems; }
    public void setLineItems(List<OrderLine> lineItems) { this.lineItems = lineItems; }
}
