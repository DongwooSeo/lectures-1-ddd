package com.growmighty.lectures.firstday.tangledmonolith.payment.infrastructure;

import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.Payment;
import com.growmighty.lectures.firstday.tangledmonolith.payment.domain.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        return jpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
