package com.outforpavan.orderflow.orders.api;

import com.outforpavan.orderflow.orders.persistence.OrderStore;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderStore orders;
    private final CircuitBreakerRegistry breakers;

    public OrderController(OrderStore orders, CircuitBreakerRegistry breakers) {
        this.orders = orders;
        this.breakers = breakers;
    }

    @PostMapping
    public ResponseEntity<OrderView> create(@RequestHeader("Idempotency-Key") String key,
                                            @Valid @RequestBody CreateOrderRequest request,
                                            JwtAuthenticationToken principal) {
        OrderView order = orders.create(principal.getToken().getSubject(), key, request);
        return ResponseEntity.accepted().location(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/{id}")
    public OrderView read(@PathVariable UUID id, JwtAuthenticationToken principal) {
        boolean admin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
        return orders.read(id, principal.getToken().getSubject(), admin);
    }

    @GetMapping("/_operations/breakers")
    @PreAuthorize("hasRole('admin')")
    public Map<String, String> breakers(Authentication principal) {
        Map<String, String> states = new TreeMap<>();
        breakers.getAllCircuitBreakers().forEach(b -> states.put(b.getName(), b.getState().name()));
        return states;
    }
}
