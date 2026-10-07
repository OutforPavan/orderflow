package com.outforpavan.orderflow.inventory.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReservationRequest(
        @NotNull UUID orderId,
        @NotNull @Min(1) Long productId,
        @NotNull @Min(1) @Max(1_000_000) Integer quantity) {}
