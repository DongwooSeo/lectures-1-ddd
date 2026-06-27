package com.growmighty.lectures.firstday.tangledmonolith.payment.infrastructure;

import com.growmighty.lectures.firstday.tangledmonolith.payment.application.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FakePaymentGateway implements PaymentGateway {
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public PgApproval approve(BigDecimal amount) {
        String transactionId = "PG-" + sequence.getAndIncrement();
        return new PgApproval(transactionId);
    }

    @Override
    public void cancel(String pgTransactionId) {
    }
}
