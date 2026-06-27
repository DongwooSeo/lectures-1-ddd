package com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client;

import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.PaymentPort;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.dto.PaymentResult;
import com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client.dto.ApiResponseBody;
import com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client.dto.PaymentApiData;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentHttpClient implements PaymentPort {

    private final RestClient orderRestClient;

    @Override
    public PaymentResult pay(BigDecimal amount) {
        ApiResponseBody<PaymentApiData> body = orderRestClient.post()
                .uri("/payments?amount={amount}", amount)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        PaymentApiData data = body.data();
        return new PaymentResult(data.paymentId(), data.amount(), data.status());
    }

    @Override
    public void cancel(Long paymentId) {
        orderRestClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .retrieve()
                .toBodilessEntity();
    }
}
