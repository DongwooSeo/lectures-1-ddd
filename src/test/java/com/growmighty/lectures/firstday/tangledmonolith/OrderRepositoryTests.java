package com.growmighty.lectures.firstday.tangledmonolith;

import com.growmighty.lectures.firstday.tangledmonolith.order.domain.Order;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderItem;
import com.growmighty.lectures.firstday.tangledmonolith.order.domain.OrderRepository;
import com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.OrderRepositoryAdapter;
import com.growmighty.lectures.firstday.tangledmonolith.product.domain.Product;
import com.growmighty.lectures.firstday.tangledmonolith.seller.domain.Seller;
import com.growmighty.lectures.firstday.tangledmonolith.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderRepositoryAdapter.class)
class OrderRepositoryTests {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("주문 저장 및 조회 테스트")
    void saveAndFindOrderTest() {
        User user = User.register("dannyseo@growmighty", "aaa33242", "Danny", "010-0000-0000");
        entityManager.persist(user);

        Seller seller = Seller.apply(user.getId(), "테스트 셀러");
        entityManager.persist(seller);

        Product product = Product.register(seller.getId(), "Dofia 이동식 접이식 식탁 의자 4개 세트 가정용 소형주택 신축식", BigDecimal.valueOf(179000), 10, "test");
        entityManager.persist(product);

        List<OrderItem> items = new ArrayList<>();
        items.add(OrderItem.create(product.getName(), product.getPrice(), product.getId(), 1));
        Order order = Order.create(user.getId(), items);

        Order saved = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getItems()).hasSize(1);
        OrderItem foundItem = found.getItems().get(0);
        assertThat(foundItem.getQuantity()).isEqualTo(1);
        assertThat(foundItem.getProductId()).isEqualTo(product.getId());
        assertThat(foundItem.getName()).isEqualTo(product.getName());
        assertThat(foundItem.getPrice().getValue()).isEqualByComparingTo(product.getPrice());
    }
}
