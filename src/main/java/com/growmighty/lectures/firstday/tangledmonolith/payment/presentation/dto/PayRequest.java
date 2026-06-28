package com.growmighty.lectures.firstday.tangledmonolith.payment.presentation.dto;

import lombok.NonNull;

import java.math.BigDecimal;

public record PayRequest(@NonNull BigDecimal amount) {
}
