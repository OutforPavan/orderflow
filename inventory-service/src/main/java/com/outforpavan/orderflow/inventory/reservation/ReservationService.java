package com.outforpavan.orderflow.inventory.reservation;

import com.outforpavan.orderflow.inventory.availability.AvailabilityService;
import com.outforpavan.orderflow.inventory.product.Product;
import com.outforpavan.orderflow.inventory.product.ProductRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final ProductRepository products;
    private final AvailabilityService availability;

    public ReservationService(ReservationRepository reservations, ProductRepository products,
                              AvailabilityService availability) {
        this.reservations = reservations;
        this.products = products;
        this.availability = availability;
    }

    @Transactional(timeout = 3)
    public Reservation reserve(ReservationRequest request) {
        reservations.lockOrder(request.orderId());
        Reservation existing = reservations.find(request.orderId()).orElse(null);
        if (existing != null) {
            if (existing.productId() == null) {
                // A cancellation reached us first. Bind its input for collision detection,
                // but never resurrect the cancelled operation or decrement stock.
                reservations.bindTombstone(request);
                return reservations.find(request.orderId()).orElseThrow();
            }
            if (!existing.productId().equals(request.productId()) || !existing.quantity().equals(request.quantity())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order ID already has different reservation input");
            }
            return existing;
        }

        // The cached public observation is intentionally absent from the write decision.
        Product product = products.lock(request.productId()).orElse(null);
        if (product == null) {
            return reservations.insert(new Reservation(request.orderId(), request.productId(), request.quantity(),
                    null, ReservationStatus.REJECTED, "PRODUCT_NOT_FOUND"));
        }
        if (product.stock() < request.quantity()) {
            return reservations.insert(new Reservation(request.orderId(), request.productId(), request.quantity(),
                    product.price(), ReservationStatus.REJECTED, "INSUFFICIENT_STOCK"));
        }

        products.decrement(product.id(), request.quantity());
        Reservation result = reservations.insert(new Reservation(request.orderId(), product.id(), request.quantity(),
                product.price(), ReservationStatus.RESERVED, null));
        availability.evictAfterCommit(product.id());
        return result;
    }

    @Transactional(timeout = 3)
    public Reservation release(UUID orderId) {
        reservations.lockOrder(orderId);
        Reservation existing = reservations.find(orderId).orElse(null);
        if (existing == null) {
            return reservations.insert(new Reservation(orderId, null, null, null,
                    ReservationStatus.RELEASED, "CANCELLED_BEFORE_RESERVATION"));
        }
        if (existing.status() == ReservationStatus.RELEASED) {
            return existing;
        }
        if (existing.status() == ReservationStatus.RESERVED) {
            products.lock(existing.productId())
                    .orElseThrow(() -> new IllegalStateException("Reserved product is missing"));
            products.restore(existing.productId(), existing.quantity());
            availability.evictAfterCommit(existing.productId());
        }
        // A rejected operation never held stock, so it needs no stock adjustment.
        return reservations.released(existing);
    }

    public Reservation get(UUID orderId) {
        return reservations.find(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }
}
