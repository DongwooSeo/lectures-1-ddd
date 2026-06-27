package com.growmighty.lectures.firstday.tangledmonolith.order.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.OrderApiService;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.OrderFacade;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.OrderResponse;
import com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderCommunicationController {

    private final OrderFacade orderFacade;
    private final OrderApiService orderApiService;

    @PostMapping("/facade")
    public ApiResponse<OrderResponse> placeOrderViaFacade(@RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(OrderResponse.from(orderFacade.placeOrder(request.toCommand())));
    }

    @PostMapping("/api")
    public ApiResponse<OrderResponse> placeOrderViaApi(@RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(OrderResponse.from(orderApiService.placeOrder(request.toCommand())));
    }
}
