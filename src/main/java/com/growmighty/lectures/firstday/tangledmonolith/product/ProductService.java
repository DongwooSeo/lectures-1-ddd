package com.growmighty.lectures.firstday.tangledmonolith.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + productId));
    }

    public List<Product> getProducts(Set<Long> productIds) {
        if(productIds == null || productIds.isEmpty())
            throw new IllegalArgumentException("상품 Ids가 존재하지 않습니다.");

        return productRepository.findByIdIn(productIds);
    }
}
