package com.growmighty.lectures.firstday.tangledmonolith.product.domain;

import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long id);
}
