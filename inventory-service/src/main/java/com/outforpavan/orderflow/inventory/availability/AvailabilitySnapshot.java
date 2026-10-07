package com.outforpavan.orderflow.inventory.availability;

import java.time.Instant;

public record AvailabilitySnapshot(long productId, int available, Instant observedAt) {}
