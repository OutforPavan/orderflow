package com.outforpavan.orderflow.inventory.availability;

import java.time.Instant;

/** This is an advisory observation, never permission to reserve or sell stock. */
public record AvailabilityResponse(long productId, int available, Instant observedAt, boolean cached) {}
