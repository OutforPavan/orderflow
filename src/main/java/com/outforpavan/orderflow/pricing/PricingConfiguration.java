package com.outforpavan.orderflow.pricing;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfiguration {
    @Bean
    public PriceCalculator priceCalculator() {
        return new PriceCalculator();
    }

    @Bean
    public ProductDiscountPolicy productDiscountPolicy(
            @Value("${pricing.discount-all-products:false}") boolean allProducts,
            @Value("${pricing.discount-product-ids:}") String configuredIds) {
        Set<Long> ids = new HashSet<Long>();
        if (!configuredIds.trim().isEmpty()) {
            for (String id : configuredIds.split(",", -1)) {
                ids.add(Long.valueOf(id.trim()));
            }
        }
        return new ProductDiscountPolicy(allProducts, ids);
    }
}
