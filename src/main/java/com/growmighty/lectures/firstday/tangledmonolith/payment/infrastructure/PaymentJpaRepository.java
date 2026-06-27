package com.growmighty.lectures.firstday.tangledmonolith.payment.infrastructure;

import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
}
