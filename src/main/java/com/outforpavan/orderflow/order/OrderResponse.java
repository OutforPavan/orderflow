package com.outforpavan.orderflow.order;

import com.outforpavan.orderflow.pricing.ServiceLevel;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(Long id, Long productId, int quantity,
                            BigDecimal unitPrice, BigDecimal total, Instant createdAt,
                            BigDecimal subtotal, BigDecimal discountPercent, BigDecimal discountAmount,
                            BigDecimal prioritySurchargePercent, BigDecimal prioritySurchargeAmount,
                            ServiceLevel serviceLevel) {
    static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(),
                order.getUnitPrice(), order.getTotal(), order.getCreatedAt(),
                order.getSubtotal(), order.getDiscountPercent(), order.getDiscountAmount(),
                order.getPrioritySurchargePercent(), order.getPrioritySurchargeAmount(), order.getServiceLevel());
    }
}
