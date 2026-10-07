package com.outforpavan.orderflow.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.outforpavan.orderflow.inventory.availability.AvailabilityService;
import com.outforpavan.orderflow.inventory.availability.AvailabilitySnapshot;
import com.outforpavan.orderflow.inventory.product.Product;
import com.outforpavan.orderflow.inventory.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class AvailabilityFailureTest {
    @Test
    void cachedAdvisoryCanSurviveATemporaryDatabaseOutageButExpiresWithoutInventingStock() {
        AtomicLong elapsed = new AtomicLong();
        ProductRepository products = mock(ProductRepository.class);
        Cache<Long, AvailabilitySnapshot> cache = Caffeine.newBuilder()
                .maximumSize(1000).expireAfterWrite(Duration.ofSeconds(5)).ticker(elapsed::get).build();
        AvailabilityService availability = new AvailabilityService(products, cache, Clock.systemUTC());
        when(products.find(1L)).thenReturn(Optional.of(new Product(1, "Keyboard", new BigDecimal("100.00"), 3)))
                .thenThrow(new DataAccessResourceFailureException("simulated database connection outage"));

        assertThat(availability.get(1).cached()).isFalse();
        assertThat(availability.get(1).available()).isEqualTo(3);
        verify(products, times(1)).find(1L);

        elapsed.set(Duration.ofSeconds(6).toNanos());
        assertThatThrownBy(() -> availability.get(1)).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(cache.getIfPresent(1L)).isNull();
        verify(products, times(2)).find(1L);
    }
}
