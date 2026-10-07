package com.outforpavan.orderflow.product;

import com.outforpavan.orderflow.api.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    protected Product() {
        // JPA needs a no-argument constructor to hydrate database rows.
    }

    public Product(String name, BigDecimal price, int stock) {
        this.name = Objects.requireNonNull(name);
        changePrice(price);
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > stock) {
            throw new InsufficientStockException(id, quantity, stock);
        }
        stock -= quantity;
    }

    public void changePrice(BigDecimal price) {
        Objects.requireNonNull(price);
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.price = price;
    }
}
