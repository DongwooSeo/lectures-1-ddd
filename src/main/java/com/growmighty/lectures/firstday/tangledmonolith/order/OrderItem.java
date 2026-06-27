package com.growmighty.lectures.firstday.tangledmonolith.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name="value", column = @Column(name="price", nullable = false))
    private Money price;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    public static OrderItem create(String name, BigDecimal price, Long productId, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.name = name;
        orderItem.price = Money.from(price);
        orderItem.productId = productId;
        orderItem.quantity = quantity;

        return orderItem;
    }

    public void changePrice(BigDecimal newPrice) {
        this.price = Money.from(newPrice);
    }

    public void changeQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}
