package com.growmighty.lectures.firstday.tangledmonolith.order.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.OrderService;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.ChangeOrderItemPriceRequest;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.ChangeOrderItemQuantityRequest;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.OrderConsistencyResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.OrderResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.PlaceOrderFromCartRequest;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders() {
        List<OrderResponse> responses = orderService.getOrders().stream()
                .map(OrderResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(OrderResponse.from(orderService.placeOrder(request.toCommand())));
    }

    @PostMapping("/from-cart")
    public ApiResponse<OrderResponse> placeOrderFromCart(@RequestBody PlaceOrderFromCartRequest request) {
        return ApiResponse.ok(OrderResponse.from(orderService.placeOrderFromCart(request.userId())));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(OrderResponse.from(orderService.cancelOrder(orderId)));
    }

    @GetMapping("/{orderId}/inspect")
    public ApiResponse<OrderConsistencyResponse> inspectOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(OrderConsistencyResponse.from(orderService.inspectOrder(orderId)));
    }

    @PatchMapping("/{orderId}/orderItems/{orderItemId}/price")
    public ApiResponse<Void> changeOrderItemPrice(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestBody ChangeOrderItemPriceRequest request) {
        orderService.changeItemPrice(orderId, orderItemId, request.price());
        return ApiResponse.ok();
    }

    @PatchMapping("/{orderId}/orderItems/{orderItemId}/quantity")
    public ApiResponse<Void> changeOrderItemQuantity(@PathVariable Long orderId, @PathVariable Long orderItemId, @RequestBody ChangeOrderItemQuantityRequest request) {
        orderService.changeItemQuantity(orderId, orderItemId, request.quantity());
        return ApiResponse.ok();
    }
}
