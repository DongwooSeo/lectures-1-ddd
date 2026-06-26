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

            if (product.getStockQuantity() < quantity) {
                throw new IllegalStateException("재고가 부족합니다. product=" + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - quantity);
            
            OrderItem orderItem = OrderItem.create(product.getName(), product.getPrice(), product.getId(), quantity);
            orderItems.add(orderItem);

            BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(lineAmount);
        }

        Order order = Order.create(user.getId(), orderItems);
        order.setTotalAmount(totalAmount);

        Payment payment = Payment.ready(totalAmount);
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        order.assignPayment(payment);
        order.setStatus(OrderStatus.PAID);

        Order saved = orderRepository.save(order);
        return new OrderResult(saved.getId());
    }

    public List<OrderResult> getOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(e -> new OrderResult(e.getId())).toList();
    }
}
