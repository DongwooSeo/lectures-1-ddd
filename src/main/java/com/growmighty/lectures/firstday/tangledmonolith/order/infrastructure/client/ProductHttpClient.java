package com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client;

import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.ProductPort;
import com.growmighty.lectures.firstday.tangledmonolith.order.application.port.dto.ProductSnapshot;
import com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client.dto.ApiResponseBody;
import com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client.dto.ProductApiData;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductHttpClient implements ProductPort {

    private final RestClient orderRestClient;

    @Override
    public ProductSnapshot getProduct(Long productId) {
        ApiResponseBody<ProductApiData> body = orderRestClient.get()
                .uri("/products/{productId}", productId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        ProductApiData data = body.data();
        return new ProductSnapshot(
                data.id(),
                data.name(),
                data.price(),
                data.stockQuantity(),
                data.orderable());
    }

    @Override
    public void decreaseStock(Long productId, int quantity) {
        orderRestClient.post()
                .uri("/products/{productId}/decrease-stock?quantity={quantity}", productId, quantity)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void restoreStock(Long productId, int quantity) {
        orderRestClient.post()
                .uri("/products/{productId}/restore-stock?quantity={quantity}", productId, quantity)
                .retrieve()
                .toBodilessEntity();
    }
}
