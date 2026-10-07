package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;
import java.util.Collections;

/** Run directly without Spring; the five dependency-free pricing/demo classes use Java 8 APIs. */
public final class PricingDemo {
    private PricingDemo() {
    }

    public static void main(String[] args) {
        BigDecimal catalogPrice = new BigDecimal("1000.00");
        PriceCalculator calculator = new PriceCalculator();
        ProductDiscountPolicy campaign = new ProductDiscountPolicy(
                false, Collections.singleton(101L));

        for (ServiceLevel serviceLevel : ServiceLevel.values()) {
            PriceBreakdown price = calculator.calculate(catalogPrice, 1,
                    campaign.getDiscountPercent(101L), serviceLevel);
            print("Eligible product 101", price);
        }
        print("Ineligible product 202", calculator.calculate(catalogPrice, 1,
                campaign.getDiscountPercent(202L), ServiceLevel.PRIORITY_5));
        System.out.println("Catalog price is still " + catalogPrice.toPlainString());
    }

    private static void print(String label, PriceBreakdown price) {
        System.out.println(label + " / " + price.getServiceLevel()
                + ": subtotal=" + price.getSubtotal().toPlainString()
                + ", discount=" + price.getDiscountAmount().toPlainString()
                + ", priority fee=" + price.getPrioritySurchargeAmount().toPlainString()
                + ", total=" + price.getTotal().toPlainString());
    }
}
