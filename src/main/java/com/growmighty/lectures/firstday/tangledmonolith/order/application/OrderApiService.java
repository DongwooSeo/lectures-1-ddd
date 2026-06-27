package com.growmighty.lectures.firstday.tangledmonolith.order.application;

import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderLine;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderResult;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.PlaceOrderCommand;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.PaymentPort;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.ProductPort;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.dto.PaymentResult;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.dto.ProductSnapshot;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderItem;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderApiService {

    private final OrderRepository orderRepository;
    private final ProductPort productPort;
    private final PaymentPort paymentPort;

    public OrderResult placeOrder(PlaceOrderCommand command) {
        List<OrderLine> lines = command.lines();
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderLine line : lines) {
            ProductSnapshot product = productPort.getProduct(line.productId());
            if (!product.orderable()) {
                throw new IllegalStateException("현재 구매할 수 없는 상품입니다. productId=" + product.productId());
            }
            orderItems.add(OrderItem.create(product.name(), product.price(), product.productId(), line.quantity()));
            productPort.decreaseStock(line.productId(), line.quantity());
        }

        Order order = Order.create(command.userId(), orderItems);

        PaymentResult payment = paymentPort.pay(order.getTotalAmount().getValue());
        order.completePayment(payment.paymentId());

        return OrderResult.from(orderRepository.save(order));
    }
}
