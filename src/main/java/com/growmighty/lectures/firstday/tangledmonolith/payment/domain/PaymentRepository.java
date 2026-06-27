package com.growmighty.lectures.firstday.tangledmonolith.payment.domain;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(Long id);
}
