package com.outforpavan.orderflow.inventory.product;

import java.math.BigDecimal;

public record Product(long id, String name, BigDecimal price, int stock) {}
