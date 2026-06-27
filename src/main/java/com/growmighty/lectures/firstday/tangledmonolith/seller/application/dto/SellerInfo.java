package com.growmighty.lectures.firstday.tangledmonolith.seller.application.dto;

import com.growmighty.lectures.firstday.tangledmonolith.seller.domain.Seller;
import com.growmighty.lectures.firstday.tangledmonolith.seller.domain.SellerStatus;

public record SellerInfo(
        Long id,
        Long userId,
        String businessName,
        SellerStatus status
) {
    public static SellerInfo from(Seller seller) {
        return new SellerInfo(seller.getId(), seller.getUserId(), seller.getBusinessName(), seller.getStatus());
    }
}
