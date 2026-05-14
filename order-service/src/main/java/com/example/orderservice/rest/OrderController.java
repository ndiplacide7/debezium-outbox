package com.example.orderservice.rest;

import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /orders
     * Body: { "customerId": 1, "orderDate": "2024-01-15T10:00:00", "lineItems": [...] }
     *
     * Creates the order AND writes OrderCreated + InvoiceCreated to the Outbox
     * in a single DB transaction. Debezium picks up the outbox events and
     * publishes them to Kafka automatically.
     */
    @PostMapping
    public ResponseEntity<OrderOperationResponse> addOrder(@Valid @RequestBody CreateOrderRequest request) {
        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(request.getOrderDate());
        order.setLineItems(request.getLineItems());

        PurchaseOrder saved = orderService.addOrder(order);
        return ResponseEntity.ok(OrderOperationResponse.from(saved));
    }

    /**
     * PUT /orders/{orderId}/lines/{lineId}
     * Body: { "newStatus": "CANCELLED" }
     *
     * Updates an order line status AND writes OrderLineUpdated to the Outbox.
     */
    @PutMapping("/{orderId}/lines/{lineId}")
    public ResponseEntity<OrderOperationResponse> updateOrderLine(
            @PathVariable long orderId,
            @PathVariable long lineId,
            @Valid @RequestBody UpdateOrderLineRequest request) {

        PurchaseOrder updated = orderService.updateOrderLine(orderId, lineId, request.getNewStatus());
        return ResponseEntity.ok(OrderOperationResponse.from(updated));
    }
}