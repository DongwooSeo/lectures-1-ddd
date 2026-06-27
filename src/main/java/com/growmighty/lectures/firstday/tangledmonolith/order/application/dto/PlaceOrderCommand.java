package com.growmighty.lectures.firstday.tangledmonolith.order.application.dto;

import java.util.List;

public record PlaceOrderCommand(Long userId, List<OrderLine> lines) {
}
