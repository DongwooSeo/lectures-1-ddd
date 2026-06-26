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
    public OrderResult placeOrder(@RequestBody OrderRequest request) {
        return orderService.placeOrder(request.userId(), request.items());
    }

    // 주문 항목 손댄 다음 총액 맞는지 확인용
    @GetMapping("/{orderId}/inspect")
    public OrderConsistencyView inspectOrder(@PathVariable Long orderId) {
        return orderService.inspectOrder(orderId);
    }

    // 항목 가격만 바꾸는 API (Order 안 거치고 바로 바꿔서 총액이 틀어지는 걸 보여주는 용도)
    @PatchMapping("/orderItems/{orderItemId}/price")
    public void changeOrderItemPrice(@PathVariable Long orderItemId, @RequestParam BigDecimal price) {
        orderService.changeItemPrice(orderItemId, price);
    }

    // 수량만 바꾸는 API. 위와 똑같이 총액은 그대로 남는다.
    @PatchMapping("/orderItems/{orderItemId}/quantity")
    public void changeOrderItemQuantity(@PathVariable Long orderItemId, @RequestParam int quantity) {
        orderService.changeItemQuantity(orderItemId, quantity);
    }

    public record OrderRequest(@NonNull Long userId, @NonNull List<OrderLine> items) {

    }

    public record OrderLine(@NonNull Long productId, @NonNull Integer quantity) {

    }
}
