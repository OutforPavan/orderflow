package com.outforpavan.orderflow.orders.client;

import java.math.BigDecimal;
import java.util.UUID;

public interface InventoryClient {
    Reservation reserve(UUID orderId, long productId, int quantity);
    Reservation release(UUID orderId);

    record ReserveRequest(UUID orderId, long productId, int quantity) { }
    record Reservation(UUID orderId, Long productId, Integer quantity, BigDecimal unitPrice, String status, String reason) { }
}
