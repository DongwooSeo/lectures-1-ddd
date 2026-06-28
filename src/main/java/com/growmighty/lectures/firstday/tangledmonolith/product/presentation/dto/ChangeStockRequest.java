package com.growmighty.lectures.firstday.tangledmonolith.product.presentation.dto;

import lombok.NonNull;

public record ChangeStockRequest(@NonNull Integer quantity) {
}
