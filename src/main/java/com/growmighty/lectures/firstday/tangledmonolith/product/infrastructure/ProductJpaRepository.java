package com.growmighty.lectures.firstday.tangledmonolith.product.infrastructure;

import com.growmighty.lectures.firstday.tangledmonolith.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
}
