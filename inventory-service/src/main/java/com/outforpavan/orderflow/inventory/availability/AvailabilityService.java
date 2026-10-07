package com.outforpavan.orderflow.inventory.availability;

import com.github.benmanes.caffeine.cache.Cache;
import com.outforpavan.orderflow.inventory.product.Product;
import com.outforpavan.orderflow.inventory.product.ProductRepository;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvailabilityService {
    private final ProductRepository products;
    private final Cache<Long, AvailabilitySnapshot> cache;
    private final Clock clock;

    public AvailabilityService(ProductRepository products, Cache<Long, AvailabilitySnapshot> cache, Clock clock) {
        this.products = products;
        this.cache = cache;
        this.clock = clock;
    }

    public AvailabilityResponse get(long id) {
        AtomicBoolean loaded = new AtomicBoolean();
        AvailabilitySnapshot value = cache.get(id, key -> {
            loaded.set(true);
            Product product = products.find(key)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            return new AvailabilitySnapshot(product.id(), product.stock(), clock.instant());
        });
        return new AvailabilityResponse(value.productId(), value.available(), value.observedAt(), !loaded.get());
    }

    /** Evict after commit, avoiding an early eviction followed by a refill of the old committed stock. */
    public void evictAfterCommit(long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.invalidate(id);
                }
            });
        } else {
            cache.invalidate(id);
        }
    }
}
