package com.growmighty.lectures.firstday.tangledmonolith.cart.presentation.dto;

import lombok.NonNull;

public record ChangeCartItemQuantityRequest(@NonNull Integer quantity) {
}
