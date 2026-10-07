package com.outforpavan.orderflow.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PaymentIntegrationTest {
    @Autowired PaymentService payments;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanIsolatedTestDatabase() {
        jdbc.execute("truncate table simulated_payment_ledger, payments");
    }

    @Test
    void approvedPaymentIsDurableAndReplayReturnsTheSamePaymentWithoutASecondDebit() {
        var request = request(UUID.randomUUID(), "alice", "123.40", "demo-approved");
        PaymentResponse created = payments.create(request);
        PaymentResponse replay = payments.create(request);

        assertThat(created.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(replay).isEqualTo(created);
        assertThat(payments.find(request.orderId())).isEqualTo(created);
        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select amount from simulated_payment_ledger", BigDecimal.class))
                .isEqualByComparingTo("123.40");
    }

    @Test
    void numericScaleDoesNotChangeIdempotentIdentity() {
        UUID orderId = UUID.randomUUID();
        var created = payments.create(request(orderId, "alice", "1.0", "demo-approved"));
        var replay = payments.create(request(orderId, "alice", "1.00", "demo-approved"));
        assertThat(replay.paymentId()).isEqualTo(created.paymentId());
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
    }

    @Test
    void concurrentIdenticalOperationsAcrossTransactionsProduceOneLedgerDebit() throws Exception {
        var request = request(UUID.randomUUID(), "alice", "99.50", "demo-approved");
        var start = new CountDownLatch(1);
        List<Future<PaymentResponse>> futures = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(12)) {
            for (int i = 0; i < 12; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return payments.create(request);
                }));
            }
            start.countDown();
            UUID paymentId = futures.getFirst().get(30, TimeUnit.SECONDS).paymentId();
            for (var future : futures) {
                assertThat(future.get(30, TimeUnit.SECONDS).paymentId()).isEqualTo(paymentId);
            }
        }
        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
    }

    @Test
    void changedAmountCustomerOrMethodConflictsWithoutAnotherDebit() {
        UUID orderId = UUID.randomUUID();
        var original = request(orderId, "alice", "49.00", "demo-approved");
        payments.create(original);
        var conflicts = List.of(
                request(orderId, "alice", "50.00", "demo-approved"),
                request(orderId, "bob", "49.00", "demo-approved"),
                request(orderId, "alice", "49.00", "demo-declined"));
        for (var changed : conflicts) {
            assertThatThrownBy(() -> payments.create(changed))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        }
        assertThat(payments.find(orderId).amount()).isEqualByComparingTo("49.00");
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
    }

    @Test
    void competingDifferentPayloadsHaveOneWinnerAndOneConflict() throws Exception {
        UUID orderId = UUID.randomUUID();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> race(start, request(orderId, "alice", "10.00", "demo-approved")));
            var second = pool.submit(() -> race(start, request(orderId, "alice", "20.00", "demo-approved")));
            start.countDown();
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
    }

    @Test
    void declineIsDurableAndDoesNotCreateASimulatedDebit() {
        var request = request(UUID.randomUUID(), "alice", "30.00", "demo-declined");
        var first = payments.create(request);
        assertThat(first.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(first.reason()).isEqualTo("SIMULATED_PAYMENT_DECLINED");
        assertThat(payments.create(request)).isEqualTo(first);
        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("simulated_payment_ledger")).isZero();
    }

    @Test
    void unknownSimulatedMethodNeverInventsASuccess() {
        var response = payments.create(request(UUID.randomUUID(), "alice", "30.00", "unknown-method"));
        assertThat(response.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.reason()).isEqualTo("UNSUPPORTED_SIMULATED_PAYMENT_METHOD");
        assertThat(count("simulated_payment_ledger")).isZero();
    }

    @Test
    void localSimulatorDebitRollsBackIfPersistingThePaymentOutcomeFails() {
        // A real external provider cannot be rolled back by this transaction.
        jdbc.execute("""
                create function reject_test_payment() returns trigger language plpgsql as $$
                begin raise exception 'Deliberate test failure after simulated debit'; end; $$
                """);
        jdbc.execute("""
                create trigger reject_test_payment before insert on payments
                for each row execute function reject_test_payment()
                """);
        var request = request(UUID.randomUUID(), "alice", "19.00", "demo-approved");
        try {
            assertThatThrownBy(() -> payments.create(request)).isInstanceOf(DataAccessException.class);
            assertThat(count("payments")).isZero();
            assertThat(count("simulated_payment_ledger")).isZero();
        } finally {
            jdbc.execute("drop trigger reject_test_payment on payments");
            jdbc.execute("drop function reject_test_payment()");
        }
        assertThat(payments.create(request).status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(count("simulated_payment_ledger")).isEqualTo(1);
    }

    @Test
    void ownerAndAdminCanReadWhileAnotherCustomerGetsTheSame404AsAMissingPayment() {
        var payment = payments.create(request(UUID.randomUUID(), "alice", "23.00", "demo-approved"));
        assertThat(payments.findForCustomer(payment.orderId(), "alice", false)).isEqualTo(payment);
        assertThat(payments.findForCustomer(payment.orderId(), "admin-subject", true)).isEqualTo(payment);
        assertNotFound(() -> payments.findForCustomer(payment.orderId(), "bob", false));
        assertNotFound(() -> payments.findForCustomer(UUID.randomUUID(), "alice", false));
    }

    @Test
    void invalidAmountsAreRejectedBeforeEitherTableIsWritten() {
        for (String amount : List.of("0", "-1.00", "1.001", "100000000000000000.00")) {
            assertThatThrownBy(() -> payments.create(request(UUID.randomUUID(), "alice", amount, "demo-approved")))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        }
        assertThat(count("payments")).isZero();
        assertThat(count("simulated_payment_ledger")).isZero();
    }

    private int race(CountDownLatch start, CreatePaymentRequest request) throws InterruptedException {
        start.await();
        try {
            payments.create(request);
            return 200;
        } catch (ResponseStatusException conflict) {
            return conflict.getStatusCode().value();
        }
    }

    private void assertNotFound(Runnable read) {
        assertThatThrownBy(read::run).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(404);
            assertThat(error.getReason()).isEqualTo("Payment not found");
        });
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private CreatePaymentRequest request(UUID orderId, String customer, String amount, String method) {
        return new CreatePaymentRequest(orderId, customer, new BigDecimal(amount), method);
    }
}
