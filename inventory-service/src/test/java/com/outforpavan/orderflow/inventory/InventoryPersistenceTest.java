package com.outforpavan.orderflow.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.outforpavan.orderflow.inventory.availability.AvailabilityResponse;
import com.outforpavan.orderflow.inventory.availability.AvailabilityService;
import com.outforpavan.orderflow.inventory.availability.AvailabilitySnapshot;
import com.outforpavan.orderflow.inventory.product.ChangePriceRequest;
import com.outforpavan.orderflow.inventory.product.CreateProductRequest;
import com.outforpavan.orderflow.inventory.product.Product;
import com.outforpavan.orderflow.inventory.product.ProductService;
import com.outforpavan.orderflow.inventory.reservation.Reservation;
import com.outforpavan.orderflow.inventory.reservation.ReservationRequest;
import com.outforpavan.orderflow.inventory.reservation.ReservationService;
import com.outforpavan.orderflow.inventory.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

/** Real PostgreSQL transactions: do not wrap the test method in an ambient transaction. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(InventoryPersistenceTest.TestTime.class)
class InventoryPersistenceTest {
    @Autowired ProductService products;
    @Autowired ReservationService reservations;
    @Autowired AvailabilityService availability;
    @Autowired Cache<Long, AvailabilitySnapshot> cache;
    @Autowired MutableTicker ticker;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetIsolatedTestDatabase() {
        jdbc.execute("TRUNCATE TABLE reservations, products RESTART IDENTITY");
        cache.invalidateAll();
        cache.cleanUp();
        ticker.reset();
    }

    @Test
    void productCreationAndPriceChangePreserveStock() {
        Product product = product(8);
        Product changed = products.changePrice(product.id(), new ChangePriceRequest(new BigDecimal("89.99")));
        assertThat(changed.price()).isEqualByComparingTo("89.99");
        assertThat(products.get(product.id()).stock()).isEqualTo(8);
    }

    @Test
    void duplicateReservationKeepsOriginalPriceAndDoesNotDecrementAgain() {
        Product product = product(5);
        ReservationRequest request = request(product, 2);
        Reservation original = reservations.reserve(request);
        products.changePrice(product.id(), new ChangePriceRequest(new BigDecimal("250.00")));
        Reservation replay = reservations.reserve(request);

        assertThat(original.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(replay).isEqualTo(original);
        assertThat(replay.unitPrice()).isEqualByComparingTo("100.00");
        assertThat(products.get(product.id()).stock()).isEqualTo(3);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void orderIdCannotBeReusedWithChangedQuantityOrProduct() {
        Product first = product(10);
        Product second = product(10);
        ReservationRequest request = request(first, 2);
        reservations.reserve(request);

        assertConflict(() -> reservations.reserve(new ReservationRequest(request.orderId(), first.id(), 3)));
        assertConflict(() -> reservations.reserve(new ReservationRequest(request.orderId(), second.id(), 2)));
        assertThat(products.get(first.id()).stock()).isEqualTo(8);
        assertThat(products.get(second.id()).stock()).isEqualTo(10);
    }

    @Test
    void rejectedReservationIsDurableEvenWhenStockLaterBecomesAvailable() {
        Product product = product(0);
        ReservationRequest request = request(product, 1);
        Reservation rejected = reservations.reserve(request);
        jdbc.update("UPDATE products SET stock = 3 WHERE id = ?", product.id());

        assertThat(rejected.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(rejected.reason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(reservations.reserve(request)).isEqualTo(rejected);
        assertThat(products.get(product.id()).stock()).isEqualTo(3);
    }

    @Test
    void missingProductHasAStoredRejectionInsteadOfAnUntrackedFailure() {
        ReservationRequest request = new ReservationRequest(UUID.randomUUID(), 999L, 1);
        Reservation rejected = reservations.reserve(request);
        assertThat(rejected.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(rejected.reason()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reservations.get(request.orderId())).isEqualTo(rejected);
        assertThat(reservations.reserve(request)).isEqualTo(rejected);
    }

    @Test
    void simultaneousDuplicateRequestsHaveOneStockEffect() throws Exception {
        Product product = product(10);
        ReservationRequest request = request(product, 2);
        List<Reservation> results = concurrently(12, () -> reservations.reserve(request));

        assertThat(results).allMatch(result -> result.status() == ReservationStatus.RESERVED);
        assertThat(results).allMatch(result -> result.equals(results.getFirst()));
        assertThat(products.get(product.id()).stock()).isEqualTo(8);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void competingOrdersCannotOversellTheLastFiveItems() throws Exception {
        Product product = product(5);
        List<Reservation> results = concurrently(20, () -> reservations.reserve(request(product, 1)));

        assertThat(results.stream().filter(r -> r.status() == ReservationStatus.RESERVED)).hasSize(5);
        assertThat(results.stream().filter(r -> r.status() == ReservationStatus.REJECTED)).hasSize(15);
        assertThat(products.get(product.id()).stock()).isZero();
        assertThat(rowCount()).isEqualTo(20);
    }

    @Test
    void concurrentCompensationRestoresStockExactlyOnce() throws Exception {
        Product product = product(5);
        ReservationRequest request = request(product, 3);
        reservations.reserve(request);
        List<Reservation> results = concurrently(12, () -> reservations.release(request.orderId()));

        assertThat(results).allMatch(result -> result.status() == ReservationStatus.RELEASED);
        assertThat(products.get(product.id()).stock()).isEqualTo(5);
        assertThat(reservations.reserve(request).status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(products.get(product.id()).stock()).isEqualTo(5);
    }

    @Test
    void releaseBeforeReserveLeavesATombstoneThatPreventsLateStockConsumption() {
        Product product = product(5);
        ReservationRequest request = request(product, 3);
        Reservation tombstone = reservations.release(request.orderId());
        assertThat(tombstone.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(tombstone.productId()).isNull();

        Reservation late = reservations.reserve(request);
        assertThat(late.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(late.productId()).isEqualTo(product.id());
        assertThat(late.quantity()).isEqualTo(3);
        assertThat(products.get(product.id()).stock()).isEqualTo(5);
        assertThat(reservations.reserve(request)).isEqualTo(late);
        assertConflict(() -> reservations.reserve(new ReservationRequest(request.orderId(), product.id(), 1)));
    }

    @Test
    void racingInitialReserveAndReleaseAlwaysEndReleasedWithFullStock() throws Exception {
        Product product = product(5);
        for (int attempt = 0; attempt < 10; attempt++) {
            ReservationRequest request = request(product, 2);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CountDownLatch start = new CountDownLatch(1);
                Future<Reservation> reserve = executor.submit(() -> {
                    start.await();
                    return reservations.reserve(request);
                });
                Future<Reservation> release = executor.submit(() -> {
                    start.await();
                    return reservations.release(request.orderId());
                });
                start.countDown();
                reserve.get(20, TimeUnit.SECONDS);
                release.get(20, TimeUnit.SECONDS);
            }
            assertThat(reservations.get(request.orderId()).status()).isEqualTo(ReservationStatus.RELEASED);
            assertThat(products.get(product.id()).stock()).isEqualTo(5);
        }
    }

    @Test
    void aFailedReservationInsertRollsBackTheAlreadyExecutedStockDecrement() {
        Product product = product(5);
        installFault("BEFORE INSERT", "NEW.status = 'RESERVED'");
        try {
            assertThatThrownBy(() -> reservations.reserve(request(product, 2))).isInstanceOf(DataAccessException.class);
        } finally {
            removeFault();
        }
        assertThat(products.get(product.id()).stock()).isEqualTo(5);
        assertThat(rowCount()).isZero();
    }

    @Test
    void aFailedReleaseStateUpdateRollsBackTheStockRestoration() {
        Product product = product(5);
        ReservationRequest request = request(product, 2);
        reservations.reserve(request);
        installFault("BEFORE UPDATE", "NEW.status = 'RELEASED'");
        try {
            assertThatThrownBy(() -> reservations.release(request.orderId())).isInstanceOf(DataAccessException.class);
        } finally {
            removeFault();
        }
        assertThat(products.get(product.id()).stock()).isEqualTo(3);
        assertThat(reservations.get(request.orderId()).status()).isEqualTo(ReservationStatus.RESERVED);
        reservations.release(request.orderId());
        assertThat(products.get(product.id()).stock()).isEqualTo(5);
    }

    @Test
    void availabilityUsesTheCacheAndReservationAndReleaseInvalidateAfterCommit() {
        Product product = product(5);
        long hitsBefore = cache.stats().hitCount();
        AvailabilityResponse first = availability.get(product.id());
        AvailabilityResponse second = availability.get(product.id());
        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(second.observedAt()).isEqualTo(first.observedAt());
        assertThat(cache.stats().hitCount()).isEqualTo(hitsBefore + 1);

        ReservationRequest request = request(product, 2);
        reservations.reserve(request);
        AvailabilityResponse reserved = availability.get(product.id());
        assertThat(reserved.available()).isEqualTo(3);
        assertThat(reserved.cached()).isFalse();

        reservations.release(request.orderId());
        AvailabilityResponse restored = availability.get(product.id());
        assertThat(restored.available()).isEqualTo(5);
        assertThat(restored.cached()).isFalse();
    }

    @Test
    void cacheExpiryReloadsAnotherReplicasChangeWithoutSleeping() {
        Product product = product(5);
        availability.get(product.id());
        // Simulate an update made by a different inventory instance, which cannot evict this cache.
        jdbc.update("UPDATE products SET stock = 2 WHERE id = ?", product.id());
        ticker.advance(Duration.ofSeconds(4));
        AvailabilityResponse stale = availability.get(product.id());
        assertThat(stale.cached()).isTrue();
        assertThat(stale.available()).isEqualTo(5);

        ticker.advance(Duration.ofSeconds(2));
        AvailabilityResponse refreshed = availability.get(product.id());
        assertThat(refreshed.cached()).isFalse();
        assertThat(refreshed.available()).isEqualTo(2);
    }

    @Test
    void reservationBypassesAnOptimisticCachedStockObservation() {
        Product product = product(5);
        availability.get(product.id());
        jdbc.update("UPDATE products SET stock = 0 WHERE id = ?", product.id());
        assertThat(availability.get(product.id()).available()).isEqualTo(5);

        Reservation result = reservations.reserve(request(product, 1));
        assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(products.get(product.id()).stock()).isZero();
    }

    @Test
    void priceChangeEvictsAndMissingProductsAreNotNegativelyCached() {
        Product product = product(5);
        availability.get(product.id());
        products.changePrice(product.id(), new ChangePriceRequest(new BigDecimal("80.00")));
        assertThat(availability.get(product.id()).cached()).isFalse();
        assertThatThrownBy(() -> availability.get(999)).isInstanceOf(ResponseStatusException.class);
        assertThat(cache.getIfPresent(999L)).isNull();
    }

    @Test
    void cacheDoesNotGrowWithoutBoundWhenManyProductsAreRead() {
        jdbc.update("""
                INSERT INTO products(name, price, stock)
                SELECT 'Cache fixture ' || n, 1.00, 1 FROM generate_series(1, 1100) n
                """);
        for (long id = 1; id <= 1100; id++) {
            availability.get(id);
        }
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(1000);
        assertThat(cache.stats().evictionCount()).isPositive();
    }

    private Product product(int stock) {
        return products.create(new CreateProductRequest("Test keyboard", new BigDecimal("100.00"), stock));
    }

    private ReservationRequest request(Product product, int quantity) {
        return new ReservationRequest(UUID.randomUUID(), product.id(), quantity);
    }

    private int rowCount() {
        return jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class);
    }

    private void assertConflict(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ResponseStatusException.class,
                failure -> assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private <T> List<T> concurrently(int count, Callable<T> operation) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch ready = new CountDownLatch(count);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return operation.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }

    private void installFault(String timing, String condition) {
        jdbc.execute("""
                CREATE FUNCTION fail_inventory_test_write() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                  RAISE EXCEPTION 'deliberate inventory test write failure';
                END $$
                """);
        jdbc.execute("CREATE TRIGGER inventory_test_fault " + timing + " ON reservations FOR EACH ROW WHEN ("
                + condition + ") EXECUTE FUNCTION fail_inventory_test_write()");
    }

    private void removeFault() {
        jdbc.execute("DROP FUNCTION fail_inventory_test_write() CASCADE");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestTime {
        @Bean
        @Primary
        MutableTicker testTicker() {
            return new MutableTicker();
        }
    }

    static class MutableTicker implements Ticker {
        private final AtomicLong nanoseconds = new AtomicLong();

        @Override
        public long read() {
            return nanoseconds.get();
        }

        void advance(Duration duration) {
            nanoseconds.addAndGet(duration.toNanos());
        }

        void reset() {
            nanoseconds.set(0);
        }
    }
}
