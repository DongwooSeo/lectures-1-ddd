package com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto;

import lombok.NonNull;

public record PlaceOrderFromCartRequest(@NonNull Long userId) {
}
