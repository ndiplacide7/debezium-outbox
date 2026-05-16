package com.example.orderservice.rest;

import com.example.orderservice.model.PurchaseOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response returned after any order operation")
public class OrderOperationResponse {

    @Schema(description = "Generated order ID", example = "1")
    private Long id;

    @Schema(description = "Customer who placed the order", example = "42")
    private long customerId;

    @Schema(description = "Date and time the order was placed", example = "2026-05-14T10:30:00")
    private LocalDateTime orderDate;

    @Schema(description = "Current state of all order lines")
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
