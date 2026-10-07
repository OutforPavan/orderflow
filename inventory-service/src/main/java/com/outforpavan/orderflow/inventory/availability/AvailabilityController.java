package com.outforpavan.orderflow.inventory.availability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/availability")
public class AvailabilityController {
    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/{id}")
    public AvailabilityResponse get(@PathVariable long id) {
        return availability.get(id);
    }
}
