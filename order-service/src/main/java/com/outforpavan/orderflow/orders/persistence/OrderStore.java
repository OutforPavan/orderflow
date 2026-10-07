package com.outforpavan.orderflow.orders.persistence;

import com.outforpavan.orderflow.orders.api.CreateOrderRequest;
import com.outforpavan.orderflow.orders.api.OrderView;
import com.outforpavan.orderflow.pricing.PriceBreakdown;
import com.outforpavan.orderflow.pricing.ServiceLevel;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class OrderStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final int leaseSeconds;

    public OrderStore(JdbcTemplate jdbc, TransactionTemplate transactions,
                      @Value("${saga.lease-seconds:15}") int leaseSeconds) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.leaseSeconds = leaseSeconds;
    }

    public OrderView create(String customerId, String key, CreateOrderRequest request) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,120}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1-120 safe characters");
        }
        String hash = fingerprint(request);
        return transactions.execute(status -> {
            jdbc.update("""
                    INSERT INTO checkout_orders(id,customer_id,idempotency_key,request_hash,product_id,quantity,
                      service_level,payment_method_reference,state) VALUES (?,?,?,?,?,?,?,?,'RESERVE_PENDING')
                    ON CONFLICT (customer_id,idempotency_key) DO NOTHING
                    """, UUID.randomUUID(), customerId, key, hash, request.productId(), request.quantity(),
                    request.serviceLevel().name(), request.paymentMethodReference());
            StoredOrder order = jdbc.queryForObject("SELECT * FROM checkout_orders WHERE customer_id=? AND idempotency_key=?",
                    this::map, customerId, key);
            if (!order.requestHash().equals(hash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was used for a different request");
            }
            return order.view();
        });
    }

    public OrderView read(UUID id, String customerId, boolean admin) {
        OrderView view = find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).view();
        if (!admin && !view.customerId().equals(customerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return view;
    }

    public Optional<StoredOrder> find(UUID id) {
        return jdbc.query("SELECT * FROM checkout_orders WHERE id=?", this::map, id).stream().findFirst();
    }

    public Optional<StoredOrder> claim() {
        UUID owner = UUID.randomUUID();
        // Claim is one short database transaction. No database lock survives an HTTP call.
        return jdbc.query("""
                WITH candidate AS (
                  SELECT id FROM checkout_orders
                  WHERE state IN ('RESERVE_PENDING','PAYMENT_PENDING','RELEASE_PENDING')
                    AND next_attempt_at <= clock_timestamp()
                    AND (lease_until IS NULL OR lease_until < clock_timestamp())
                  ORDER BY next_attempt_at,created_at FOR UPDATE SKIP LOCKED LIMIT 1
                ) UPDATE checkout_orders o SET lease_owner=?,
                    lease_until=clock_timestamp() + (? * interval '1 second'), updated_at=clock_timestamp()
                  FROM candidate c WHERE o.id=c.id RETURNING o.*
                """, this::map, owner, leaseSeconds).stream().findFirst();
    }

    public boolean reserved(StoredOrder order, BigDecimal unitPrice, PriceBreakdown price) {
        return jdbc.update("""
                UPDATE checkout_orders SET state='PAYMENT_PENDING', unit_price=?,subtotal=?,discount_percent=?,
                  discount_amount=?,priority_surcharge_percent=?,priority_surcharge_amount=?,total=?,
                  reason=NULL,attempts=0,next_attempt_at=clock_timestamp(),lease_owner=NULL,lease_until=NULL,
                  updated_at=clock_timestamp() WHERE id=? AND lease_owner=? AND state='RESERVE_PENDING'
                """, unitPrice, price.getSubtotal(), price.getDiscountPercent(), price.getDiscountAmount(),
                price.getPrioritySurchargePercent(), price.getPrioritySurchargeAmount(), price.getTotal(),
                order.view().id(), order.leaseOwner()) == 1;
    }

    public boolean advance(StoredOrder order, String nextState, String reason) {
        return jdbc.update("""
                UPDATE checkout_orders SET state=?,reason=?,attempts=0,next_attempt_at=clock_timestamp(),
                  lease_owner=NULL,lease_until=NULL,updated_at=clock_timestamp()
                  WHERE id=? AND lease_owner=? AND state=?
                """, nextState, reason, order.view().id(), order.leaseOwner(), order.view().state()) == 1;
    }

    public void retry(StoredOrder order, String reason) {
        // Persist the attempt and delay so retries survive restarts. Delay is capped.
        int delay = Math.min(30, 1 << Math.min(order.view().attempts(), 5));
        jdbc.update("""
                UPDATE checkout_orders SET attempts=attempts+1,reason=?,
                  next_attempt_at=clock_timestamp() + (? * interval '1 second'), lease_owner=NULL,
                  lease_until=NULL,updated_at=clock_timestamp() WHERE id=? AND lease_owner=? AND state=?
                """, reason, delay, order.view().id(), order.leaseOwner(), order.view().state());
    }

    private StoredOrder map(ResultSet rs, int ignored) throws SQLException {
        OrderView view = new OrderView(rs.getObject("id", UUID.class), rs.getString("customer_id"),
                rs.getLong("product_id"), rs.getInt("quantity"), ServiceLevel.valueOf(rs.getString("service_level")),
                rs.getString("state"), rs.getBigDecimal("unit_price"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount_percent"), rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("priority_surcharge_percent"), rs.getBigDecimal("priority_surcharge_amount"),
                rs.getBigDecimal("total"), rs.getString("reason"), rs.getInt("attempts"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
        return new StoredOrder(view, rs.getString("request_hash"), rs.getString("payment_method_reference"),
                rs.getObject("lease_owner", UUID.class));
    }

    private static String fingerprint(CreateOrderRequest request) {
        String canonical = request.productId() + "|" + request.quantity() + "|" + request.serviceLevel()
                + "|" + request.paymentMethodReference();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public record StoredOrder(OrderView view, String requestHash, String paymentMethodReference, UUID leaseOwner) { }
}
