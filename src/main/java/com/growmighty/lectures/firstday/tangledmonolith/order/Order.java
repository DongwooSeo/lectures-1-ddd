package com.growmighty.lectures.firstday.tangledmonolith.order;

import com.growmighty.lectures.firstday.tangledmonolith.payment.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items = new ArrayList<>();

    @Column
    private Long paymentId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private Order(Long userId, List<OrderItem> items) {
        validateItems(items);
        items.forEach(this::addOrderItem);
        this.userId = userId;
        this.status = OrderStatus.CREATED;
        this.totalAmount = calculateTotalAmount(items);
    }

    public static Order create(Long userId, List<OrderItem> items) {
        return new Order(userId, items);
    }

    private void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("주문할 상품이 없습니다.");
        }
    }

    private void addOrderItem(OrderItem item) {
        this.items.add(item);

        if (item.getOrder() != this) {
            item.assignOrder(this);
        }
    }

    private Money calculateTotalAmount(List<OrderItem> items) {
        return Money.from(items.stream()
                .map(e -> e.getPrice().getValue().multiply(BigDecimal.valueOf(e.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public void completePayment(Long paymentId) {
        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
    }

    public void changeItemPrice(Long orderItemId, BigDecimal newPrice) {
        OrderItem target = items.stream()
                .filter(e -> e.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("주문에 없는 항목입니다."));

        target.changePrice(newPrice);
        this.totalAmount = calculateTotalAmount(items);
    }

    public void changeItemQuantity(Long orderItemId, int newQuantity) {
        OrderItem target = items.stream()
                .filter(e -> e.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("주문에 없는 항목입니다."));

        target.changeQuantity(newQuantity);
        this.totalAmount = calculateTotalAmount(items);
    }

}
