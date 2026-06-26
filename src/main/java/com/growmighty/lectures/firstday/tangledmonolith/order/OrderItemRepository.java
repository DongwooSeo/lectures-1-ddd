package com.growmighty.lectures.firstday.tangledmonolith.order;

import org.springframework.data.jpa.repository.JpaRepository;

// OrderItem 단건을 다루려고 만든 레포지토리.
// 근데 이게 생기니까 Order 안 거치고 OrderItem 을 바로 찾아서 고치고 저장하게 된다.
// 그러다 보면 Order 의 총액이랑 안 맞는 일이 생김. 이래도 되나 싶은 부분.
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
