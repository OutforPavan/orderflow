package com.outforpavan.orderflow.inventory.reservation;

import java.math.BigDecimal;
import java.util.UUID;

/** A release-before-reserve tombstone has null productId/quantity until the first reserve arrives. */
public record Reservation(
        UUID orderId, Long productId, Integer quantity, BigDecimal unitPrice,
        ReservationStatus status, String reason) {}
