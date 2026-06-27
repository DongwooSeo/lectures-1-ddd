package com.growmighty.lectures.firstday.tangledmonolith.payment.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.payment.application.PaymentService;
import com.growmighty.lectures.firstday.tangledmonolith.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ApiResponse.ok(PaymentResponse.from(paymentService.getPayment(paymentId)));
    }

    @PostMapping
    public ApiResponse<PaymentResponse> pay(@RequestParam BigDecimal amount) {
        return ApiResponse.ok(PaymentResponse.from(paymentService.pay(amount)));
    }

    @PostMapping("/{paymentId}/cancel")
    public ApiResponse<PaymentResponse> cancel(@PathVariable Long paymentId) {
        return ApiResponse.ok(PaymentResponse.from(paymentService.cancel(paymentId)));
    }
}
