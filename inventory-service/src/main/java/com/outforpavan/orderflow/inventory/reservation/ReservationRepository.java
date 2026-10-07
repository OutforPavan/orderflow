package com.outforpavan.orderflow.inventory.reservation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationRepository {
    private static final RowMapper<Reservation> MAPPER = (rs, row) -> new Reservation(
            rs.getObject("order_id", UUID.class), rs.getObject("product_id", Long.class),
            rs.getObject("quantity", Integer.class), rs.getBigDecimal("unit_price"),
            ReservationStatus.valueOf(rs.getString("status")), rs.getString("reason"));
    private final JdbcTemplate jdbc;

    public ReservationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * A row lock cannot lock a row that does not yet exist. This transaction-scoped advisory
     * lock serializes both first creation and release-before-reserve for the same order.
     * A hash collision only serializes unrelated orders; it cannot merge their stored keys.
     */
    public void lockOrder(UUID orderId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> null, orderId.toString());
    }

    public Optional<Reservation> find(UUID orderId) {
        return jdbc.query("""
                SELECT order_id, product_id, quantity, unit_price, status, reason
                FROM reservations WHERE order_id = ?
                """, MAPPER, orderId).stream().findFirst();
    }

    public Reservation insert(Reservation value) {
        jdbc.update("""
                INSERT INTO reservations(order_id, product_id, quantity, unit_price, status, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """, value.orderId(), value.productId(), value.quantity(), value.unitPrice(),
                value.status().name(), value.reason());
        return value;
    }

    public void bindTombstone(ReservationRequest request) {
        jdbc.update("""
                UPDATE reservations SET product_id = ?, quantity = ?, updated_at = CURRENT_TIMESTAMP
                WHERE order_id = ? AND status = 'RELEASED' AND product_id IS NULL
                """, request.productId(), request.quantity(), request.orderId());
    }

    public Reservation released(Reservation value) {
        jdbc.update("""
                UPDATE reservations SET status = 'RELEASED', reason = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE order_id = ?
                """, value.orderId());
        return new Reservation(value.orderId(), value.productId(), value.quantity(), value.unitPrice(),
                ReservationStatus.RELEASED, "CANCELLED");
    }
}
