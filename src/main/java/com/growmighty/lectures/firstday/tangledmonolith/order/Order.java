package com.growmighty.lectures.firstday.tangledmonolith.order;

import com.growmighty.lectures.firstday.tangledmonolith.payment.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // 총액. setter 가 public 이라 order.setTotalAmount(0) 같은 것도 못 막는다.
    // "총액 = 단가*수량 합계" 라는 규칙을 지켜줄 데가 지금은 없음.
    @Setter
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 그냥 받은 대로 만드는 팩토리.
    // 원래 주문이면 지켜야 할 게 있는데(항목 없이는 주문 X, 항목 최대 10개 등) 여기엔 아무 검증이 없다.
    // 총액 계산도 여기서 안 하고 OrderService 가 setter 로 넣어주는 구조.
    public static Order create(Long userId, List<OrderItem> items) {
        Order order = new Order();
        order.userId = userId;
        order.status = OrderStatus.CREATED;
        order.totalAmount = BigDecimal.ZERO;

        for (OrderItem item : items) {
            order.addOrderItem(item);
        }

        return order;
    }

    public void assignPayment(Long paymentId) {
        this.paymentId = paymentId;
    }

    private void addOrderItem(OrderItem item) {
        this.items.add(item);

        if (item.getOrder() != this) {
            item.assignOrder(this);
        }
    }
}
