package com.outforpavan.orderflow.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(Long id, Long productId, int quantity,
                            BigDecimal unitPrice, BigDecimal total, Instant createdAt) {
    static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(),
                order.getUnitPrice(), order.getTotal(), order.getCreatedAt());
    }
}
