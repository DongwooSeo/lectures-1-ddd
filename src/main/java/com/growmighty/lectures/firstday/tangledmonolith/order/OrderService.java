package com.growmighty.lectures.firstday.tangledmonolith.order;

import com.growmighty.lectures.firstday.tangledmonolith.payment.Payment;
import com.growmighty.lectures.firstday.tangledmonolith.payment.PaymentRepository;
import com.growmighty.lectures.firstday.tangledmonolith.payment.PaymentStatus;
import com.growmighty.lectures.firstday.tangledmonolith.product.Product;
import com.growmighty.lectures.firstday.tangledmonolith.product.ProductRepository;
import com.growmighty.lectures.firstday.tangledmonolith.user.User;
import com.growmighty.lectures.firstday.tangledmonolith.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    // OrderItem 을 직접 다루려고 주입해둠 (Order 안 거치고 항목 수정하는 데 씀)
    private final OrderItemRepository orderItemRepository;

    // 주문 생성.
    // 재고 확인하고 깎기, 단가*수량 합산, 총액 계산까지 전부 여기서 처리한다.
    // 쓰다 보니 OrderService 가 너무 많은 걸 알고 있고 금액 계산도 BigDecimal 로 여기저기 흩어져 있음.
    // 재고는 원래 Product 일, 총액은 Order 일인데 다 끌어와서 처리하는 중.
    @Transactional
    public OrderResult placeOrder(Long userId, List<OrderController.OrderLine> items) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. userId=" + userId));

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("주문할 상품이 없습니다.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderController.OrderLine item : items) {
            Long productId = item.productId();
            int quantity = item.quantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + productId));

            // 재고 확인하고 깎는 것도 사실 Product 가 알아서 할 일인데 여기서 한다
            if (product.getStockQuantity() < quantity) {
                throw new IllegalStateException("재고가 부족합니다. product=" + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - quantity);

            OrderItem orderItem = OrderItem.create(product.getName(), product.getPrice(), product.getId(), quantity);
            orderItems.add(orderItem);

            // 라인 금액도 그냥 BigDecimal 로 곱하고 더함
            BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(lineAmount);
        }

        Order order = Order.create(user.getId(), orderItems);
        // 총액을 Order 가 스스로 계산 안 하고 밖에서 넣어준다
        order.setTotalAmount(totalAmount);

        Payment payment = Payment.ready(totalAmount);
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        order.assignPayment(payment.getId());
        order.setStatus(OrderStatus.PAID);

        Order saved = orderRepository.save(order);
        return new OrderResult(saved.getId());
    }

    public List<OrderResult> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(e -> new OrderResult(e.getId())).toList();
    }

    // 주문 항목 가격 변경.
    // OrderItem 만 바로 찾아서 price 를 바꾸는데 Order.totalAmount 는 안 건드린다.
    // 그래서 이거 호출하고 나면 저장된 결제금액이랑 실제 합계가 따로 논다. inspectOrder 로 확인 가능.
    @Transactional
    public void changeItemPrice(Long orderItemId, BigDecimal newPrice) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 항목입니다. orderItemId=" + orderItemId));

        orderItem.setPrice(newPrice);
        orderItemRepository.save(orderItem);
    }

    // 수량 변경도 마찬가지. 항목만 바뀌고 총액은 그대로 남는다.
    @Transactional
    public void changeItemQuantity(Long orderItemId, int newQuantity) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 항목입니다. orderItemId=" + orderItemId));

        orderItem.setQuantity(newQuantity);
        orderItemRepository.save(orderItem);
    }

    // 저장된 총액이랑 항목으로 다시 계산한 총액을 비교해본다.
    // changeItemPrice / changeItemQuantity 부르고 나서 이걸 호출하면 consistent 가 false 로 떨어진다.
    // 비교는 compareTo 로 해야 함. equals 쓰면 1000 이랑 1000.00 이 다르다고 나와서 또 틀린다.
    @Transactional(readOnly = true)
    public OrderConsistencyView inspectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        BigDecimal storedTotal = order.getTotalAmount();

        BigDecimal recalculatedTotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            recalculatedTotal = recalculatedTotal.add(
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        boolean consistent = storedTotal.compareTo(recalculatedTotal) == 0;
        return new OrderConsistencyView(orderId, storedTotal, recalculatedTotal, consistent);
    }
}
