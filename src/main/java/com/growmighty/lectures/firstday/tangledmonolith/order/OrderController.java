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

    // 주문 항목 손댄 다음 총액 맞는지 확인용
    @GetMapping("/{orderId}/inspect")
    public OrderConsistencyView inspectOrder(@PathVariable Long orderId) {
        return orderService.inspectOrder(orderId);
    }

    // 항목 가격만 바꾸는 API (Order 안 거치고 바로 바꿔서 총액이 틀어지는 걸 보여주는 용도)
    @PatchMapping("/{orderId}/orderItems/{orderItemId}/price")
    public void changeOrderItemPrice(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestParam BigDecimal price) {
        orderService.changeItemPrice(orderId, orderItemId, price);
    }

    // 수량만 바꾸는 API. 위와 똑같이 총액은 그대로 남는다.
    @PatchMapping("/{orderId}/orderItems/{orderItemId}/quantity")
    public void changeOrderItemQuantity(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestParam int quantity) {
        orderService.changeItemQuantity(orderId, orderItemId, quantity);
    }

    public record OrderCreationRequest(@NonNull Long userId, @NonNull List<OrderItemRequest> requests) {

    }

    public record OrderItemRequest(@NonNull Long productId, @NonNull Integer quantity) {

    }
}
