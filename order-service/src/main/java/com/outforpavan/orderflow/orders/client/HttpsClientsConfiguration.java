package com.outforpavan.orderflow.orders.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class HttpsClientsConfiguration {
    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5)).permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(HttpClientErrorException.class).build());
    }

    private RestClient client(String baseUrl, SslBundles bundles, Duration connect, Duration read) {
        if (!"https".equals(URI.create(baseUrl).getScheme())) {
            throw new IllegalArgumentException("Internal service URL must use HTTPS");
        }
        HttpClient http = HttpClient.newBuilder().sslContext(bundles.getBundle("client").createSslContext())
                .connectTimeout(connect).followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(read);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean
    InventoryClient inventoryClient(SslBundles bundles, CircuitBreakerRegistry registry,
            @Value("${clients.inventory-url}") String url,
            @Value("${clients.connect-timeout:1s}") Duration connect,
            @Value("${clients.read-timeout:2s}") Duration read) {
        RestClient client = client(url, bundles, connect, read);
        CircuitBreaker breaker = registry.circuitBreaker("inventory");
        return new InventoryClient() {
            public Reservation reserve(UUID orderId, long productId, int quantity) {
                return breaker.executeSupplier(() -> Objects.requireNonNull(client.post().uri("/internal/reservations")
                        .body(new ReserveRequest(orderId, productId, quantity)).retrieve().body(Reservation.class)));
            }
            public Reservation release(UUID orderId) {
                return breaker.executeSupplier(() -> Objects.requireNonNull(client.post()
                        .uri("/internal/reservations/{id}/release", orderId).retrieve().body(Reservation.class)));
            }
        };
    }

    @Bean
    PaymentClient paymentClient(SslBundles bundles, CircuitBreakerRegistry registry,
            @Value("${clients.payment-url}") String url,
            @Value("${clients.connect-timeout:1s}") Duration connect,
            @Value("${clients.read-timeout:2s}") Duration read) {
        RestClient client = client(url, bundles, connect, read);
        CircuitBreaker breaker = registry.circuitBreaker("payment");
        return request -> breaker.executeSupplier(() -> Objects.requireNonNull(client.post().uri("/internal/payments")
                .body(request).retrieve().body(PaymentClient.Payment.class)));
    }
}
