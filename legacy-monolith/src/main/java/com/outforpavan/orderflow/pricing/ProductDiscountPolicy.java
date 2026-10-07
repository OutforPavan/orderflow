package com.outforpavan.orderflow.pricing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** A simple campaign: ten percent off every product or a configured set of products. */
public final class ProductDiscountPolicy {
    private static final BigDecimal TEN_PERCENT = new BigDecimal("10");
    private final boolean allProducts;
    private final Set<Long> eligibleProductIds;

    public ProductDiscountPolicy(boolean allProducts, Set<Long> eligibleProductIds) {
        if (eligibleProductIds == null) {
            throw new IllegalArgumentException("Eligible product IDs must not be null");
        }
        Set<Long> copy = new HashSet<Long>();
        for (Long productId : eligibleProductIds) {
            if (productId == null || productId.longValue() <= 0) {
                throw new IllegalArgumentException("Eligible product IDs must be positive");
            }
            copy.add(productId);
        }
        this.allProducts = allProducts;
        this.eligibleProductIds = Collections.unmodifiableSet(copy);
    }

    public BigDecimal getDiscountPercent(long productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        return allProducts || eligibleProductIds.contains(productId)
                ? TEN_PERCENT : BigDecimal.ZERO;
    }
}
