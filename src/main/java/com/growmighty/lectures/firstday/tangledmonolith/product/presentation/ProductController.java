package com.growmighty.lectures.firstday.tangledmonolith.product.presentation;

import com.growmighty.lectures.firstday.tangledmonolith.common.response.ApiResponse;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.ProductService;
import com.growmighty.lectures.firstday.tangledmonolith.product.presentation.dto.ProductResponse;
import com.growmighty.lectures.firstday.tangledmonolith.product.presentation.dto.RegisterProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> register(@RequestBody RegisterProductRequest request) {
        return ApiResponse.ok(ProductResponse.from(productService.register(request.toCommand())));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.ok(ProductResponse.from(productService.getProductInfo(productId)));
    }

    @PatchMapping("/{productId}/price")
    public ApiResponse<ProductResponse> changePrice(@PathVariable Long productId, @RequestParam BigDecimal price) {
        return ApiResponse.ok(ProductResponse.from(productService.changePrice(productId, price)));
    }

    @PostMapping("/{productId}/decrease-stock")
    public ApiResponse<Void> decreaseStock(@PathVariable Long productId, @RequestParam int quantity) {
        productService.decreaseStock(productId, quantity);
        return ApiResponse.ok();
    }

    @PostMapping("/{productId}/restore-stock")
    public ApiResponse<Void> restoreStock(@PathVariable Long productId, @RequestParam int quantity) {
        productService.restoreStock(productId, quantity);
        return ApiResponse.ok();
    }
}
