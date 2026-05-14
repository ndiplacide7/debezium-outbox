package com.example.orderservice.rest;

import com.example.orderservice.model.OrderLine;
import com.example.orderservice.model.OrderLineStatus;

import java.math.BigDecimal;

public class OrderLineDto {

    private Long id;
    private String item;
    private int quantity;
    private BigDecimal totalPrice;
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