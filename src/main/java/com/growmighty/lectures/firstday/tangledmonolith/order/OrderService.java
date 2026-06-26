package com.growmighty.lectures.firstday.tangledmonolith.order;

import com.growmighty.lectures.firstday.tangledmonolith.payment.Payment;
import com.growmighty.lectures.firstday.tangledmonolith.payment.PaymentService;
import com.growmighty.lectures.firstday.tangledmonolith.product.Product;
import com.growmighty.lectures.firstday.tangledmonolith.product.ProductRepository;
import com.growmighty.lectures.firstday.tangledmonolith.product.ProductService;
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
    //    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final ProductService productService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResult placeOrder(Long userId, List<OrderController.OrderItemRequest> requests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. userId=" + userId));

        List<OrderItem> orderItems = new ArrayList<>();

        requests.forEach(item -> {
            Long productId = item.productId();
            int quantity = item.quantity();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. productId=" + productId));

            product.decreaseStock(quantity);
            orderItems.add(OrderItem.create(product.getName(), product.getPrice(), product.getId(), quantity));
        });

        Order order = Order.create(user.getId(), orderItems);
        Payment payment = paymentService.pay(order.getTotalAmount().getValue());

        order.completePayment(payment.getId());
        Order savedOrder = orderRepository.save(order);
        return new OrderResult(savedOrder.getId());
    }

    public List<OrderResult> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(e -> new OrderResult(e.getId())).toList();
    }

    @Transactional
    public void changeItemPrice(Long orderId, Long orderItemId, BigDecimal newPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        order.changeItemPrice(orderItemId, newPrice);
    }

    // 수량 변경도 마찬가지. 항목만 바뀌고 총액은 그대로 남는다.
    @Transactional
    public void changeItemQuantity(Long orderId, Long orderItemId, int newQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        order.changeItemQuantity(orderItemId, newQuantity);
    }

    // 저장된 총액이랑 항목으로 다시 계산한 총액을 비교해본다.
    // changeItemPrice / changeItemQuantity 부르고 나서 이걸 호출하면 consistent 가 false 로 떨어진다.
    // 비교는 compareTo 로 해야 함. equals 쓰면 1000 이랑 1000.00 이 다르다고 나와서 또 틀린다.
    @Transactional(readOnly = true)
    public OrderConsistencyView inspectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        BigDecimal storedTotal = order.getTotalAmount().getValue();

        BigDecimal recalculatedTotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            recalculatedTotal = recalculatedTotal.add(
                    item.getPrice().getValue().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        boolean consistent = storedTotal.compareTo(recalculatedTotal) == 0;
        return new OrderConsistencyView(orderId, storedTotal, recalculatedTotal, consistent);
    }
}
