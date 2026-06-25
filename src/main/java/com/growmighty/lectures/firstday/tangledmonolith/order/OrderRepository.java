package com.growmighty.lectures.firstday.tangledmonolith.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
