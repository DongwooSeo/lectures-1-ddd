package com.growmighty.lectures.firstday.tangledmonolith.cart.presentation.dto;

import com.growmighty.lectures.firstday.tangledmonolith.cart.application.dto.AddCartItemCommand;
import lombok.NonNull;

public record AddCartItemRequest(@NonNull Long productId, @NonNull Integer quantity) {
    public AddCartItemCommand toCommand(Long userId) {
        return new AddCartItemCommand(userId, productId, quantity);
    }
}
