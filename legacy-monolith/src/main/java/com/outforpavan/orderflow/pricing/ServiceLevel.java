package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;

/** A server-defined service choice, rather than a percentage supplied by a customer. */
public enum ServiceLevel {
    STANDARD("0"),
    PRIORITY_5("5"),
    PRIORITY_10("10");

    private final BigDecimal surchargePercent;

    ServiceLevel(String surchargePercent) {
        this.surchargePercent = new BigDecimal(surchargePercent);
    }

    public BigDecimal getSurchargePercent() {
        return surchargePercent;
    }
}
