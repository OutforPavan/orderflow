package com.outforpavan.orderflow.payment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockOrder(UUID orderId) {
        // The lock is shared by all replicas and released by transaction completion.
        // Hash collisions only serialize unrelated orders; full UUIDs remain unique keys.
        long key = orderId.getMostSignificantBits() ^ orderId.getLeastSignificantBits();
        jdbc.query("select pg_advisory_xact_lock(?)", result -> { }, key);
    }

    public Optional<StoredPayment> find(UUID orderId) {
        return jdbc.query("""
                select order_id, customer_id, amount, status, payment_id, reason, request_fingerprint
                  from payments where order_id = ?
                """, (rs, row) -> new StoredPayment(map(rs), rs.getString("request_fingerprint")), orderId)
                .stream().findFirst();
    }

    public void save(PaymentResponse response, String fingerprint) {
        jdbc.update("""
                insert into payments(order_id, customer_id, amount, status, payment_id, reason, request_fingerprint)
                values (?, ?, ?, ?, ?, ?, ?)
                """, response.orderId(), response.customerId(), response.amount(), response.status().name(),
                response.paymentId(), response.reason(), fingerprint);
    }

    private PaymentResponse map(ResultSet rs) throws SQLException {
        return new PaymentResponse(rs.getObject("order_id", UUID.class), rs.getString("customer_id"),
                rs.getBigDecimal("amount"), PaymentStatus.valueOf(rs.getString("status")),
                rs.getObject("payment_id", UUID.class), rs.getString("reason"));
    }

    public record StoredPayment(PaymentResponse response, String fingerprint) {
    }
}
