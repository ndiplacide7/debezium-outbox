package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.OrderLineStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Snapshot of a single order line")
public class OrderLineDto {

    @Schema(description = "Order line ID", example = "1")
    private Long id;

    @Schema(description = "Name of the item ordered", example = "Laptop")
    private String item;

    @Schema(description = "Number of units", example = "2")
    private int quantity;

    @Schema(description = "Total price for this line", example = "1999.99")
    private BigDecimal totalPrice;

    @Schema(description = "Current line status", example = "ENTERED", allowableValues = {"ENTERED", "CANCELLED", "SHIPPED"})
    private OrderLineStatus status;

    public static OrderLineDto from(OrderLine line) {
        OrderLineDto dto = new OrderLineDto();
        dto.id = line.getId();
        dto.item = line.getItem();
        dto.quantity = line.getQuantity();
        dto.totalPrice = line.getTotalPrice();
        dto.status = line.getStatus();
        return dto;
    }

    public Long getId() { return id; }
    public String getItem() { return item; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public OrderLineStatus getStatus() { return status; }
}
