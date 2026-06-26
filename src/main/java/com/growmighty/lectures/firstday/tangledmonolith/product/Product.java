package com.growmighty.lectures.firstday.tangledmonolith.product;

import com.growmighty.lectures.firstday.tangledmonolith.seller.Seller;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Setter
    @Column(nullable = false)
    private Integer stockQuantity;

    @Lob
    private String description;

    public static Product create(Long sellerId, String name, BigDecimal price, Integer stockQuantity, String description) {
        Product product = new Product();
        product.sellerId = sellerId;
        product.name = name;
        product.price = price;
        product.stockQuantity = stockQuantity;
        product.description = description;
        return product;
    }
}
