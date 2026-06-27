package com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderClientConfig {

    @Bean
    RestClient orderRestClient(@Value("${order.client.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
