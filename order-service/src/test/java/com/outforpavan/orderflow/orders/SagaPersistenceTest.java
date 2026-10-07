package com.outforpavan.orderflow.orders;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.outforpavan.orderflow.orders.api.CreateOrderRequest;
import com.outforpavan.orderflow.orders.api.OrderView;
import com.outforpavan.orderflow.orders.client.InventoryClient;
import com.outforpavan.orderflow.orders.client.PaymentClient;
import com.outforpavan.orderflow.orders.persistence.OrderStore;
import com.outforpavan.orderflow.orders.saga.SagaWorker;
import com.outforpavan.orderflow.pricing.ServiceLevel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "pricing.discount-all-products=true")
@ActiveProfiles("test")
class SagaPersistenceTest {
    @Autowired OrderStore store;
    @Autowired SagaWorker worker;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean InventoryClient inventory;
    @MockitoBean PaymentClient payment;

    @BeforeEach void resetDatabase() { jdbc.execute("TRUNCATE checkout_orders"); }
    CreateOrderRequest request(int quantity) {
        return new CreateOrderRequest(1L, quantity, ServiceLevel.PRIORITY_10, "demo-approved");
    }
    OrderView create() { return store.create("alice", UUID.randomUUID().toString(), request(2)); }
    OrderView current(UUID id) { return store.read(id, "alice", false); }
    void due() { jdbc.update("UPDATE checkout_orders SET next_attempt_at=clock_timestamp()-interval '1 second'"); }
    void stockAvailable(OrderView order) {
        when(inventory.reserve(order.id(), 1L, 2)).thenReturn(new InventoryClient.Reservation(
                order.id(), 1L, 2, new BigDecimal("100.00"), "RESERVED", null));
    }
    PaymentClient.Payment paid(OrderView order, String status) {
        return new PaymentClient.Payment(order.id(), "alice", new BigDecimal("198.00"), status,
                UUID.randomUUID(), null);
    }

    @Test void duplicateKeyReturnsOriginalAndChangedPayloadConflicts() {
        var first = store.create("alice", "same-key", request(2));
        assertThat(store.create("alice", "same-key", request(2)).id()).isEqualTo(first.id());
        assertThatThrownBy(() -> store.create("alice", "same-key", request(3)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(store.create("bob", "same-key", request(3)).id()).isNotEqualTo(first.id());
    }

    @Test void concurrentCreatesHaveOneDurableSaga() throws Exception {
        try (var pool = Executors.newFixedThreadPool(6)) {
            var gate = new CountDownLatch(1);
            var results = new ArrayList<Future<UUID>>();
            for (int i = 0; i < 6; i++) results.add(pool.submit(() -> {
                gate.await(); return store.create("alice", "concurrent", request(2)).id();
            }));
            gate.countDown();
            var ids = new java.util.HashSet<UUID>();
            for (var result : results) ids.add(result.get(10, TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM checkout_orders", Integer.class)).isEqualTo(1);
        }
    }

    @Test void ownershipUsesAuthenticatedSubject() {
        var order = create();
        assertThatThrownBy(() -> store.read(order.id(), "bob", false)).hasMessageContaining("404");
        assertThat(store.read(order.id(), "admin-subject", true).id()).isEqualTo(order.id());
    }

    @Test void exclusiveLeaseRecoversAfterRestartAndFencesOldWorker() {
        var order = create();
        var old = store.claim().orElseThrow();
        assertThat(store.claim()).isEmpty();
        jdbc.update("UPDATE checkout_orders SET lease_until=clock_timestamp()-interval '1 second'");
        var replacement = store.claim().orElseThrow();
        assertThat(replacement.leaseOwner()).isNotEqualTo(old.leaseOwner());
        assertThat(store.advance(old, "REJECTED", "stale")).isFalse();
        assertThat(store.advance(replacement, "REJECTED", "current")).isTrue();
        assertThat(current(order.id()).reason()).isEqualTo("current");
    }

    @Test void happyPathPricesOnServerAndConfirmsOnlyAfterPayment() {
        var order = create(); stockAvailable(order);
        when(payment.pay(any())).thenReturn(paid(order, "SUCCEEDED"));
        worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("PAYMENT_PENDING");
        assertThat(current(order.id()).total()).isEqualByComparingTo("198.00");
        worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("CONFIRMED");
        assertThat(worker.processOnce()).isFalse();
        verify(payment).pay(new PaymentClient.PayRequest(order.id(), "alice", new BigDecimal("198.00"), "demo-approved"));
        verify(inventory, never()).release(any());
    }

    @Test void definiteDeclineReleasesStockAndReleaseFailureRemainsPending() {
        var order = create(); stockAvailable(order);
        when(payment.pay(any())).thenReturn(paid(order, "DECLINED"));
        when(inventory.release(order.id())).thenThrow(new ResourceAccessException("response lost"))
                .thenReturn(new InventoryClient.Reservation(order.id(), 1L, 2, new BigDecimal("100.00"), "RELEASED", null));
        worker.processOnce(); worker.processOnce(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("RELEASE_PENDING");
        due(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("CANCELLED");
        verify(inventory, times(2)).release(order.id());
    }

    @Test void lostPaymentResponseRetriesSameOperationWithoutReleasingStock() {
        var order = create(); stockAvailable(order);
        when(payment.pay(any())).thenThrow(new ResourceAccessException("committed then response lost"))
                .thenReturn(paid(order, "SUCCEEDED"));
        worker.processOnce(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("PAYMENT_PENDING");
        assertThat(current(order.id()).attempts()).isEqualTo(1);
        assertThat(worker.processOnce()).isFalse(); // Durable backoff, no tight retry loop.
        due(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("CONFIRMED");
        verify(payment, times(2)).pay(new PaymentClient.PayRequest(order.id(), "alice", new BigDecimal("198.00"), "demo-approved"));
        verify(inventory, never()).release(any());
    }

    @Test void reservationTimeoutRetainsStepAndRetriesSameOrder() {
        var order = create();
        when(inventory.reserve(order.id(), 1L, 2)).thenThrow(new ResourceAccessException("unknown"))
                .thenReturn(new InventoryClient.Reservation(order.id(), 1L, 2, new BigDecimal("100.00"), "RESERVED", null));
        worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("RESERVE_PENDING");
        due(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("PAYMENT_PENDING");
        verifyNoInteractions(payment);
    }

    @Test void insufficientStockRejectsWithoutPayment() {
        var order = create();
        when(inventory.reserve(order.id(), 1L, 2)).thenReturn(
                new InventoryClient.Reservation(order.id(), 1L, 2, null, "REJECTED", "insufficient"));
        worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("REJECTED");
        verifyNoInteractions(payment);
    }

    @Test void mismatchedPaymentResponseCannotConfirm() {
        var order = create(); stockAvailable(order);
        when(payment.pay(any())).thenReturn(new PaymentClient.Payment(UUID.randomUUID(), "alice",
                new BigDecimal("198.00"), "SUCCEEDED", UUID.randomUUID(), null));
        worker.processOnce(); worker.processOnce();
        assertThat(current(order.id()).state()).isEqualTo("PAYMENT_PENDING");
        verify(inventory, never()).release(any());
    }
}
