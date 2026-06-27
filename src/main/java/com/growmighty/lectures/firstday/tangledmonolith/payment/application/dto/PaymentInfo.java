package com.growmighty.lectures.firstday.tangledmonolith.payment.application.dto;

import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.Payment;
import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.PaymentStatus;

import java.math.BigDecimal;

public record PaymentInfo(
        Long paymentId,
        BigDecimal amount,
        PaymentStatus status
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(payment.getId(), payment.getAmount(), payment.getStatus());
    }
}
