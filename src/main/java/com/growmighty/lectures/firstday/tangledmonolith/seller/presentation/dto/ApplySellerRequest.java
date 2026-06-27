package com.growmighty.lectures.firstday.tangledmonolith.seller.presentation.dto;

import com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto.ApplySellerCommand;
import lombok.NonNull;

public record ApplySellerRequest(@NonNull Long userId, @NonNull String businessName) {
    public ApplySellerCommand toCommand() {
        return new ApplySellerCommand(userId, businessName);
    }
}
