package com.outforpavan.orderflow.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID orderId,
        String customerId,
        BigDecimal amount,
        PaymentStatus status,
        UUID paymentId,
        String reason) {
}
