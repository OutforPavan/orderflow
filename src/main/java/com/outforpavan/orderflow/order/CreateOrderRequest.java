package com.outforpavan.orderflow.order;

import com.outforpavan.orderflow.pricing.ServiceLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer quantity,
        ServiceLevel serviceLevel) {

    public CreateOrderRequest {
        serviceLevel = serviceLevel == null ? ServiceLevel.STANDARD : serviceLevel;
    }

    public CreateOrderRequest(Long productId, Integer quantity) {
        this(productId, quantity, ServiceLevel.STANDARD);
    }
}
