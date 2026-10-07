package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless pricing for a currency with two decimal places.
 * Round each order line's discount first, then charge priority on the remainder.
 * Taxes, shipping, currency conversion and payment collection are separate concerns.
 */
public final class PriceCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public PriceBreakdown calculate(BigDecimal unitPrice, int quantity,
                                    BigDecimal discountPercent, ServiceLevel serviceLevel) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        if (unitPrice.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Unit price must have at most two decimal places");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (discountPercent == null || discountPercent.signum() < 0
                || discountPercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        if (discountPercent.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Discount percentage must have at most two decimal places");
        }
        if (serviceLevel == null) {
            throw new IllegalArgumentException("Service level is required");
        }

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal normalizedDiscountPercent = discountPercent.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal discountAmount = percentageOf(subtotal, normalizedDiscountPercent);
        BigDecimal discountedAmount = subtotal.subtract(discountAmount);
        BigDecimal surchargePercent = serviceLevel.getSurchargePercent();
        BigDecimal prioritySurcharge = percentageOf(discountedAmount, surchargePercent);
        BigDecimal total = discountedAmount.add(prioritySurcharge);

        return new PriceBreakdown(subtotal, normalizedDiscountPercent, discountAmount,
                surchargePercent, prioritySurcharge, total, serviceLevel);
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
