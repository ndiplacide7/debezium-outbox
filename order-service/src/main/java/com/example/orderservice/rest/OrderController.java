package com.example.orderservice.rest;

import com.example.orderservice.model.PurchaseOrder;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "Order lifecycle management with transactional outbox event publishing")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Create a new order",
            description = """
                    Creates a purchase order with one or more line items.

                    **Outbox events published (same DB transaction):**
                    - `OrderCreated` → topic `Order.events`
                    - `InvoiceCreated` → topic `Customer.events`

                    All line items start with status `ENTERED`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
                    content = @Content(schema = @Schema(type = "object")))
    })
    @PostMapping
    public ResponseEntity<OrderOperationResponse> addOrder(@Valid @RequestBody CreateOrderRequest request) {
        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(request.getOrderDate());
        order.setLineItems(request.getLineItems());

        PurchaseOrder saved = orderService.addOrder(order);
        return ResponseEntity.ok(OrderOperationResponse.from(saved));
    }

    @Operation(
            summary = "Update an order line status",
            description = """
                    Updates the status of a specific order line.

                    **Valid status transitions:**
                    - `ENTERED` → `CANCELLED`
                    - `ENTERED` → `SHIPPED`
                    - Any → `ENTERED` (reset)

                    **Outbox event published:** `OrderLineUpdated` → topic `Order.events`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order line updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid newStatus"),
            @ApiResponse(responseCode = "404", description = "Order or order line not found")
    })
    @PutMapping("/{orderId}/lines/{lineId}")
    public ResponseEntity<OrderOperationResponse> updateOrderLine(
            @Parameter(description = "ID of the order to update", required = true, example = "1")
            @PathVariable long orderId,
            @Parameter(description = "ID of the order line to update", required = true, example = "1")
            @PathVariable long lineId,
            @Valid @RequestBody UpdateOrderLineRequest request) {

        PurchaseOrder updated = orderService.updateOrderLine(orderId, lineId, request.getNewStatus());
        return ResponseEntity.ok(OrderOperationResponse.from(updated));
    }
}
