package com.growmighty.lectures.firstday.tangledmonolith.order.application;

import com.growmighty.lectures.firstday.tangledmonolith.cart.application.CartService;
import com.growmighty.lectures.firstday.tangledmonolith.common.exception.EntityNotFoundException;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderLine;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.OrderResult;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.dto.PlaceOrderCommand;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderItem;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderRepository;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderStatus;
import com.growmighty.lectures.firstday.tangledmonolith.payment.application.PaymentService;
import com.growmighty.lectures.firstday.tangledmonolith.payment.application.dto.PaymentInfo;
import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.PaymentStatus;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.ProductService;
import com.growmighty.lectures.firstday.tangledmonolith.product.application.dto.ProductInfo;
import com.growmighty.lectures.firstday.tangledmonolith.product.domain.ProductStatus;
import com.growmighty.lectures.firstday.tangledmonolith.user.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserService userService;
    @Mock
    private ProductService productService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성: 재고 차감·결제 승인을 호출하고 결제 ID를 주문에 연결한다")
    void placeOrder_orchestratesStockAndPayment() {
        PlaceOrderCommand command = new PlaceOrderCommand(1L, List.of(new OrderLine(10L, 2)));
        when(productService.getProductInfo(10L))
                .thenReturn(new ProductInfo(10L, 1L, "원목 식탁", BigDecimal.valueOf(10_000), 5, ProductStatus.ON_SALE));
        when(paymentService.pay(any()))
                .thenReturn(new PaymentInfo(99L, BigDecimal.valueOf(23_000), PaymentStatus.PAID));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResult result = orderService.placeOrder(command);

        assertThat(result.status()).isEqualTo(OrderStatus.PAID);
        assertThat(result.totalAmount()).isEqualByComparingTo("23000");
        verify(productService).decreaseStock(10L, 2);

        ArgumentCaptor<BigDecimal> paidAmount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(paymentService).pay(paidAmount.capture());
        assertThat(paidAmount.getValue()).isEqualByComparingTo("23000");

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getPaymentId()).isEqualTo(99L);
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 생성: 라인이 비어 있으면 재고/결제를 건드리지 않고 예외가 발생한다")
    void placeOrder_emptyLines_throws() {
        PlaceOrderCommand command = new PlaceOrderCommand(1L, List.of());

        assertThatThrownBy(() -> orderService.placeOrder(command))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productService, never()).decreaseStock(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(paymentService, never()).pay(any());
    }

    @Test
    @DisplayName("주문 취소: 재고를 복원하고 결제를 취소하며 상태가 CANCELLED로 전이된다")
    void cancelOrder_restoresStockAndRefunds() {
        Order order = Order.create(1L, List.of(OrderItem.create("원목 식탁", BigDecimal.valueOf(10_000), 10L, 2)));
        order.completePayment(99L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderResult result = orderService.cancelOrder(5L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productService).restoreStock(10L, 2);
        verify(paymentService).cancel(99L);
    }

    @Test
    @DisplayName("주문 취소: 존재하지 않는 주문이면 EntityNotFoundException이 발생한다")
    void cancelOrder_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(404L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
