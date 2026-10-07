package com.outforpavan.orderflow.payment.provider;

import com.outforpavan.orderflow.payment.CreatePaymentRequest;
import com.outforpavan.orderflow.payment.PaymentStatus;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * No bank or external payment system is contacted. A ledger entry represents the
 * simulated debit and participates in the caller's local database transaction.
 * A real remote provider requires its own idempotency and reconciliation protocol.
 */
@Component
public class SimulatedPaymentProvider implements PaymentProvider {
    private final JdbcTemplate jdbc;

    public SimulatedPaymentProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Outcome charge(UUID paymentId, CreatePaymentRequest request) {
        if ("demo-declined".equals(request.paymentMethodReference())) {
            return new Outcome(PaymentStatus.DECLINED, "SIMULATED_PAYMENT_DECLINED");
        }
        if (!"demo-approved".equals(request.paymentMethodReference())) {
            return new Outcome(PaymentStatus.DECLINED, "UNSUPPORTED_SIMULATED_PAYMENT_METHOD");
        }

        jdbc.update("""
                insert into simulated_payment_ledger(order_id, payment_id, customer_id, amount)
                values (?, ?, ?, ?)
                """, request.orderId(), paymentId, request.customerId(), request.amount());
        return new Outcome(PaymentStatus.SUCCEEDED, null);
    }
}
