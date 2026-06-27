package com.growmighty.lectures.firstday.tangledmonolith.product.application;

import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.ProductInfo;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.RegisterProductCommand;
import com.growmighty.lectures.firstday.tangledmonolith.product.domain.Product;
import com.growmighty.lectures.firstday.tangledmonolith.product.domain.ProductRepository;
import com.growmighty.lectures.firstday.tangledmonolith.common.exception.EntityNotFoundException;
import com.growmighty.lectures.firstday.tangledmonolith.seller.application.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final SellerService sellerService;

    @Transactional
    public ProductInfo register(RegisterProductCommand command) {
        sellerService.validateSellable(command.sellerId());
        Product product = Product.register(
                command.sellerId(), command.name(), command.price(), command.stockQuantity(), command.description());
        return ProductInfo.from(productRepository.save(product));
    }

    @Transactional
    public ProductInfo changePrice(Long productId, BigDecimal newPrice) {
        Product product = getProductEntity(productId);
        product.changePrice(newPrice);
        return ProductInfo.from(product);
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        getProductEntity(productId).decreaseStock(quantity);
    }

    @Transactional
    public void restoreStock(Long productId, int quantity) {
        getProductEntity(productId).restoreStock(quantity);
    }

    @Transactional(readOnly = true)
    public ProductInfo getProductInfo(Long productId) {
        return ProductInfo.from(getProductEntity(productId));
    }

    private Product getProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다. productId=" + productId));
    }
}
