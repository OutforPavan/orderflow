package com.outforpavan.orderflow.orders.client;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {
    Payment pay(PayRequest request);
    record PayRequest(UUID orderId, String customerId, BigDecimal amount, String paymentMethodReference) { }
    record Payment(UUID orderId, String customerId, BigDecimal amount, String status, UUID paymentId, String reason) { }
}
