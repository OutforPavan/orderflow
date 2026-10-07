package com.outforpavan.orderflow.orders.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.outforpavan.orderflow.pricing.ServiceLevel;

public record OrderView(UUID id, String customerId, long productId, int quantity, ServiceLevel serviceLevel,
                        String state, BigDecimal unitPrice, BigDecimal subtotal, BigDecimal discountPercent,
                        BigDecimal discountAmount, BigDecimal prioritySurchargePercent,
                        BigDecimal prioritySurchargeAmount, BigDecimal total, String reason,
                        int attempts, Instant createdAt, Instant updatedAt) { }
