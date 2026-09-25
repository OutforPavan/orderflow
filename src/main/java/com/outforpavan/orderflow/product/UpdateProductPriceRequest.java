package com.outforpavan.orderflow.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateProductPriceRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
