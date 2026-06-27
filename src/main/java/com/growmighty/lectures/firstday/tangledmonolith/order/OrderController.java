package com.growmighty.lectures.firstday.tangledmonolith.order;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResult> getOrders() {
        return orderService.getOrders();
    }

    @PostMapping
    public OrderResult placeOrder(@RequestBody OrderCreationRequest request) {
        return orderService.placeOrder(request.userId(), request.requests());
    }

    @GetMapping("/{orderId}/inspect")
    public OrderConsistencyView inspectOrder(@PathVariable Long orderId) {
        return orderService.inspectOrder(orderId);
    }

    @PatchMapping("/{orderId}/orderItems/{orderItemId}/price")
    public void changeOrderItemPrice(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestParam BigDecimal price) {
        orderService.changeItemPrice(orderId, orderItemId, price);
    }

    @PatchMapping("/{orderId}/orderItems/{orderItemId}/quantity")
    public void changeOrderItemQuantity(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestParam int quantity) {
        orderService.changeItemQuantity(orderId, orderItemId, quantity);
    }

    public record OrderCreationRequest(@NonNull Long userId, @NonNull List<OrderItemRequest> requests) {

    }

    public record OrderItemRequest(@NonNull Long productId, @NonNull Integer quantity) {

    }
}
