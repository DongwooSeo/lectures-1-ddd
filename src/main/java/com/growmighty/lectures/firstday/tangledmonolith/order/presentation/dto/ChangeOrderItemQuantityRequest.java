package com.growmighty.lectures.firstday.tangledmonolith.order.presentation.dto;

import lombok.NonNull;

public record ChangeOrderItemQuantityRequest(@NonNull Integer quantity) {
}
