package com.growmighty.lectures.firstday.tangledmonolith.cart.application.dto;

public record AddCartItemCommand(Long userId, Long productId, int quantity) {
}
