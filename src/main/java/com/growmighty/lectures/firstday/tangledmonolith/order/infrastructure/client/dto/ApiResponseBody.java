package com.growmighty.lectures.firstday.tangledmonolith.order.infrastructure.client.dto;

public record ApiResponseBody<T>(boolean success, T data, ErrorBody error) {

    public record ErrorBody(String code, String message) {
    }
}
