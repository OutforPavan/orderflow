package com.outforpavan.orderflow.payment;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {
    private final PaymentService service;

    public InternalPaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return service.create(request);
    }

    @GetMapping("/{orderId}")
    public PaymentResponse status(@PathVariable UUID orderId) {
        return service.find(orderId);
    }
}
