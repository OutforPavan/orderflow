package com.outforpavan.orderflow.api;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Product " + productId + " has " + available + " item(s) available; requested " + requested);
    }
}
