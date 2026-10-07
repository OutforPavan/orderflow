package com.outforpavan.orderflow.payment;

import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/status")
public class PaymentStatusController {
    private final PaymentService service;

    public PaymentStatusController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}")
    public PaymentResponse status(@PathVariable UUID orderId, JwtAuthenticationToken authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
        return service.findForCustomer(orderId, authentication.getToken().getSubject(), admin);
    }
}
