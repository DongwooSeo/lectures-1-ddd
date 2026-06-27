package com.growmighty.lectures.firstday.tangledmonolith.order.application.port.dto;

import java.math.BigDecimal;

public record PaymentResult(
        Long paymentId,
        BigDecimal amount,
        String status
) {
}
