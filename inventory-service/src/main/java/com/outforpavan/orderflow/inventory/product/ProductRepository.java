package com.outforpavan.orderflow.inventory.product;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private static final RowMapper<Product> MAPPER = (rs, row) -> new Product(
            rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("stock"));
    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Product insert(CreateProductRequest request) {
        return jdbc.queryForObject(
                "INSERT INTO products(name, price, stock) VALUES (?, ?, ?) RETURNING id, name, price, stock",
                MAPPER, request.name().trim(), request.price(), request.stock());
    }

    public Optional<Product> find(long id) {
        return jdbc.query("SELECT id, name, price, stock FROM products WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /** Must be called inside a transaction; reservation and price changes share this row lock. */
    public Optional<Product> lock(long id) {
        return jdbc.query("SELECT id, name, price, stock FROM products WHERE id = ? FOR UPDATE", MAPPER, id)
                .stream().findFirst();
    }

    public void updatePrice(long id, BigDecimal price) {
        jdbc.update("UPDATE products SET price = ? WHERE id = ?", price, id);
    }

    public void decrement(long id, int quantity) {
        int updated = jdbc.update("UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?",
                quantity, id, quantity);
        if (updated != 1) {
            throw new IllegalStateException("Stock changed while its row was locked");
        }
    }

    public void restore(long id, int quantity) {
        if (jdbc.update("UPDATE products SET stock = stock + ? WHERE id = ?", quantity, id) != 1) {
            throw new IllegalStateException("Cannot restore reservation for a missing product");
        }
    }
}
