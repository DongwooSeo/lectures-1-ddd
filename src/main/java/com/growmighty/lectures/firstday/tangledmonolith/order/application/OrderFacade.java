package com.growmighty.lectures.firstday.tangledmonolith.order.application;

import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderLine;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderResult;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.PlaceOrderCommand;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderItem;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderRepository;
import com.growmighty.lectures.firstday.tangledmonolith.payment.application.PaymentService;
import com.growmighty.lectures.firstday.tangledmonolith.payment.application.dto.PaymentInfo;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.ProductService;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.ProductInfo;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.UserService;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResult placeOrder(PlaceOrderCommand command) {
        UserInfo user = userService.getUser(command.userId());

        List<OrderLine> lines = command.lines();
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderLine line : lines) {
            ProductInfo product = productService.getProductInfo(line.productId());
            orderItems.add(OrderItem.create(product.name(), product.price(), product.id(), line.quantity()));
            productService.decreaseStock(line.productId(), line.quantity());
        }

        Order order = Order.create(user.id(), orderItems);

        PaymentInfo payment = paymentService.pay(order.getTotalAmount().getValue());
        order.completePayment(payment.paymentId());

        return OrderResult.from(orderRepository.save(order));
    }
}
