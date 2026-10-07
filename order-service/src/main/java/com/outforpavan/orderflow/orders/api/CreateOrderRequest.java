package com.outforpavan.orderflow.orders.api;

import com.outforpavan.orderflow.pricing.ServiceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateOrderRequest(@NotNull @Min(1) Long productId,
                                 @Min(1) @Max(1000) int quantity,
                                 ServiceLevel serviceLevel,
                                 @NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,100}") String paymentMethodReference) {
    public CreateOrderRequest {
        if (serviceLevel == null) serviceLevel = ServiceLevel.STANDARD;
    }
}
