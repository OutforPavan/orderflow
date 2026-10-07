package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;

/** An immutable snapshot of one order line's calculated amounts. */
public final class PriceBreakdown {
    private final BigDecimal subtotal;
    private final BigDecimal discountPercent;
    private final BigDecimal discountAmount;
    private final BigDecimal prioritySurchargePercent;
    private final BigDecimal prioritySurchargeAmount;
    private final BigDecimal total;
    private final ServiceLevel serviceLevel;

    PriceBreakdown(BigDecimal subtotal, BigDecimal discountPercent,
                   BigDecimal discountAmount, BigDecimal prioritySurchargePercent,
                   BigDecimal prioritySurchargeAmount, BigDecimal total,
                   ServiceLevel serviceLevel) {
        this.subtotal = subtotal;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
        this.prioritySurchargePercent = prioritySurchargePercent;
        this.prioritySurchargeAmount = prioritySurchargeAmount;
        this.total = total;
        this.serviceLevel = serviceLevel;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPrioritySurchargePercent() {
        return prioritySurchargePercent;
    }

    public BigDecimal getPrioritySurchargeAmount() {
        return prioritySurchargeAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }
}
