package com.growmighty.lectures.firstday.tangledmonolith.seller.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.SellerService;
import com.growmighty.lectures.firstday.tangledmonolith.seller.presentation.dto.ApplySellerRequest;
import com.growmighty.lectures.firstday.tangledmonolith.seller.presentation.dto.SellerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers")
public class SellerController {
    private final SellerService sellerService;

    @PostMapping
    public ApiResponse<SellerResponse> apply(@RequestBody ApplySellerRequest request) {
        return ApiResponse.ok(SellerResponse.from(sellerService.apply(request.toCommand())));
    }

    @GetMapping("/{sellerId}")
    public ApiResponse<SellerResponse> getSeller(@PathVariable Long sellerId) {
        return ApiResponse.ok(SellerResponse.from(sellerService.getSeller(sellerId)));
    }

    @PostMapping("/{sellerId}/suspend")
    public ApiResponse<Void> suspend(@PathVariable Long sellerId) {
        sellerService.suspend(sellerId);
        return ApiResponse.ok();
    }
}
