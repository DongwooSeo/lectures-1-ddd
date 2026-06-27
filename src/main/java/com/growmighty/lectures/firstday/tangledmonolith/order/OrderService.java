package com.growmighty.lectures.firstday.tangledmonolith.order;

import com.growmighty.lectures.firstday.tangledmonolith.payment.Payment;
import com.growmighty.lectures.firstday.tangledmonolith.payment.PaymentService;
import com.growmighty.lectures.firstday.tangledmonolith.product.Product;
import com.growmighty.lectures.firstday.tangledmonolith.product.ProductService;
import com.growmighty.lectures.firstday.tangledmonolith.user.User;
import com.growmighty.lectures.firstday.tangledmonolith.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResult placeOrder(Long userId, List<OrderController.OrderItemRequest> requests) {
        User user = userService.getUser(userId);

        List<OrderItem> orderItems = new ArrayList<>();
        // DTO 위치
        for (OrderController.OrderItemRequest request : requests) {
            Product product = productService.getProduct(request.productId());
            orderItems.add(OrderItem.create(product.getName(), product.getPrice(), product.getId(), request.quantity()));
            product.decreaseStock(request.quantity());
        }

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

    @Transactional
    public void changeItemQuantity(Long orderId, Long orderItemId, int newQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        order.changeItemQuantity(orderItemId, newQuantity);
    }

    @Transactional(readOnly = true)
    public OrderConsistencyView inspectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. orderId=" + orderId));

        Money storedTotal = order.getTotalAmount();

        Money recalculatedTotal = order.getItems().stream()
                .map(item -> item.getPrice().times(item.getQuantity()))
                .reduce(Money.zero(), Money::plus);

        boolean consistent = storedTotal.isSameAmount(recalculatedTotal);
        return new OrderConsistencyView(orderId, storedTotal.getValue(), recalculatedTotal.getValue(), consistent);
    }
}
