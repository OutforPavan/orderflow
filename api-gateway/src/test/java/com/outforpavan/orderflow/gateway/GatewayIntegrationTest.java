package com.outforpavan.orderflow.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.ssl.enabled=false", "platform.gateway.backend-tls-enabled=false"
})
@Import(GatewayTestIdentity.class)
class GatewayIntegrationTest {
    private static final String REQUEST_ID = "X-Request-Id";
    private static final StubBackend BACKEND = new StubBackend();

    @LocalServerPort
    private int gatewayPort;

    private WebTestClient client;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry properties) {
        // Both routes use this placeholder. Replacing an indexed list entry can
        // replace the complete routes list and accidentally discard predicates.
        properties.add("ORDER_SERVICE_URL", BACKEND::baseUrl);
        properties.add("INVENTORY_SERVICE_URL", BACKEND::baseUrl);
        properties.add("PAYMENT_SERVICE_URL", BACKEND::baseUrl);
        properties.add("spring.cloud.gateway.server.webflux.httpclient.response-timeout", () -> "500ms");
        properties.add("server.address", () -> "127.0.0.1");
    }

    @BeforeEach
    void prepareClient() {
        BACKEND.reset();
        // Disable caller retries too, so the aborted POST experiment counts
        // attempts made by the gateway, not a replay by the test's HTTP client.
        client = WebTestClient.bindToServer(new ReactorClientHttpConnector(
                        HttpClient.create().disableRetry(true)))
                .baseUrl("http://127.0.0.1:" + gatewayPort)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + GatewayTestIdentity.token("admin"))
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void stopBackend() throws InterruptedException {
        BACKEND.close();
    }

    @Test
    void forwardsProductPathAndQueryWithoutRemovingApiPrefix() throws InterruptedException {
        EntityExchangeResult<String> result = client.get()
                .uri("/api/products?name=Keyboard&limit=2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class).isEqualTo("{\"source\":\"orderflow-stub\"}")
                .returnResult();

        RecordedRequest request = BACKEND.takeRequest();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/api/products");
        assertThat(request.query()).isEqualTo("name=Keyboard&limit=2");
        assertThat(request.body()).isEmpty();
        assertSameRequestId(result, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "orders"})
    void forwardsCreationBodyAndRelativeLocation(String resource) throws InterruptedException {
        String body = resource.equals("products")
                ? "{\"name\":\"Keyboard\",\"price\":1250.00,\"stock\":10}"
                : "{\"productId\":1,\"quantity\":2}";

        EntityExchangeResult<String> result = client.post()
                .uri("/api/" + resource)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/" + resource + "/42")
                .expectBody(String.class).isEqualTo("{\"id\":42}")
                .returnResult();

        RecordedRequest request = BACKEND.takeRequest();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/api/" + resource);
        assertThat(request.body()).isEqualTo(body);
        assertThat(request.contentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertSameRequestId(result, request);
    }

    @Test
    void forwardsPatchAndItsBody() throws InterruptedException {
        String body = "{\"price\":1499.00}";
        EntityExchangeResult<String> result = client.patch()
                .uri("/api/products/42/price")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(body)
                .returnResult();

        RecordedRequest request = BACKEND.takeRequest();
        assertThat(request.method()).isEqualTo("PATCH");
        assertThat(request.path()).isEqualTo("/api/products/42/price");
        assertThat(request.body()).isEqualTo(body);
        assertSameRequestId(result, request);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 500})
    void preservesBackendErrorStatusAndBody(int status) throws InterruptedException {
        EntityExchangeResult<String> result = client.get()
                .uri("/api/products/status/" + status)
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class).isEqualTo("{\"error\":\"backend-" + status + "\"}")
                .returnResult();

        assertSameRequestId(result, BACKEND.takeRequest());
        assertThat(BACKEND.requestCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/unmapped", "/api/learning/status", "/api/orders-internal",
            "/actuator/env", "/actuator/gateway/routes"})
    void doesNotForwardUnmatchedOrUnexposedPaths(String path) {
        EntityExchangeResult<String> result = client.get().uri(path)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).returnResult();

        assertGeneratedRequestId(result);
        assertThat(BACKEND.requestCount()).isZero();
    }

    @Test
    void replacesMultipleCallerAndBackendRequestIdsWithOneGeneratedId() throws InterruptedException {
        EntityExchangeResult<String> result = client.get().uri("/api/orders/42")
                .header(REQUEST_ID, "forged-client-id", "another-client-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        // The stub also returns two forged IDs. Neither side controls the edge ID.
        RecordedRequest request = BACKEND.takeRequest();
        assertSameRequestId(result, request);
        assertThat(request.requestIds()).doesNotContain("forged-client-id", "another-client-id");
    }

    @Test
    void boundsWaitingForSlowBackendAndReturns504WithRequestId() throws InterruptedException {
        long started = System.nanoTime();
        EntityExchangeResult<String> result = client.get().uri("/api/products/slow")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody(String.class).returnResult();

        // Allow scheduling overhead; the 504 itself proves the configured timeout
        // won over the stub's eventual 200 response. This is not a load benchmark.
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(4));
        assertSameRequestId(result, BACKEND.takeRequest());
        assertThat(BACKEND.requestCount()).isEqualTo(1);
    }

    @Test
    void exposesOnlyLocalHealthWithoutCallingBackend() {
        EntityExchangeResult<byte[]> result = client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components").doesNotExist()
                .jsonPath("$.details").doesNotExist()
                .returnResult();

        assertGeneratedRequestId(result);
        assertThat(BACKEND.requestCount()).isZero();
    }

    @Test
    void doesNotRepeatPostWhenBackendConsumesBodyAndDropsResponse() throws InterruptedException {
        String body = "{\"productId\":1,\"quantity\":2}";
        EntityExchangeResult<String> result = client.post().uri("/api/orders/abort")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class).returnResult();

        RecordedRequest request = BACKEND.takeRequest();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.body()).isEqualTo(body);
        assertSameRequestId(result, request);
        assertThat(BACKEND.requestCount()).isEqualTo(1);
    }

    @Test
    void rejectsMissingAndMalformedBearerBeforeReachingAnyBackend() {
        WebTestClient unauthenticated = client.mutate()
                .defaultHeaders(headers -> headers.remove(HttpHeaders.AUTHORIZATION)).build();
        for (String token : List.of("", "Bearer forged.invalid.jwt")) {
            unauthenticated.get().uri("/api/orders/42").headers(headers -> {
                if (!token.isEmpty()) headers.set(HttpHeaders.AUTHORIZATION, token);
            }).exchange().expectStatus().isUnauthorized();
        }
        assertThat(BACKEND.requestCount()).isZero();
    }

    @Test
    void validatesIssuerAudienceAndExpiryEvenWithAValidSignature() {
        List<String> invalidTokens = List.of(
                GatewayTestIdentity.token("customer", "https://wrong-issuer", "orderflow-api", java.time.Instant.now().plusSeconds(300)),
                GatewayTestIdentity.token("customer", GatewayTestIdentity.ISSUER, "other-api", java.time.Instant.now().plusSeconds(300)),
                GatewayTestIdentity.token("customer", GatewayTestIdentity.ISSUER, "orderflow-api", java.time.Instant.now().minusSeconds(120)),
                tamperedSignature(GatewayTestIdentity.token("customer")));
        for (String token : invalidTokens) {
            client.get().uri("/api/orders/42").headers(headers -> headers.setBearerAuth(token))
                    .exchange().expectStatus().isUnauthorized();
        }
        assertThat(BACKEND.requestCount()).isZero();
    }

    private String tamperedSignature(String token) {
        int start = token.lastIndexOf('.') + 1;
        return token.substring(0, start) + (token.charAt(start) == 'a' ? 'b' : 'a') + token.substring(start + 1);
    }

    @Test
    void customerCannotAdministerProductsAndNoUserCanReachInternalRoutes() {
        client.post().uri("/api/products")
                .headers(headers -> headers.setBearerAuth(GatewayTestIdentity.token("customer")))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isForbidden();
        client.get().uri("/api/orders/_operations/breakers")
                .headers(headers -> headers.setBearerAuth(GatewayTestIdentity.token("customer")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/internal/payments").exchange().expectStatus().isForbidden();
        client.get().uri("/api/orders/42")
                .headers(headers -> headers.setBearerAuth(GatewayTestIdentity.token("SERVICE_ORDER")))
                .exchange().expectStatus().isForbidden();
        assertThat(BACKEND.requestCount()).isZero();
    }

    @Test
    void customerCanReadAndBearerTokenIsForwardedUnchanged() throws InterruptedException {
        String token = GatewayTestIdentity.token("customer");
        client.get().uri("/api/orders/42").headers(headers -> headers.setBearerAuth(token))
                .exchange().expectStatus().isOk();
        assertThat(BACKEND.takeRequest().authorization()).isEqualTo("Bearer " + token);
    }

    private static String assertGeneratedRequestId(EntityExchangeResult<?> result) {
        List<String> ids = result.getResponseHeaders().get(REQUEST_ID);
        assertThat(ids).hasSize(1);
        String id = ids.getFirst();
        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        return id;
    }

    private static void assertSameRequestId(EntityExchangeResult<?> result, RecordedRequest request) {
        assertThat(request.requestIds()).containsExactly(assertGeneratedRequestId(result));
    }

    private record RecordedRequest(String method, String path, String query, String body,
                                   String contentType, List<String> requestIds, String authorization) {
    }

    private static final class StubBackend {
        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private final BlockingQueue<RecordedRequest> requests = new LinkedBlockingQueue<>();
        private final AtomicInteger count = new AtomicInteger();
        private final HttpServer server;

        private StubBackend() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException failure) {
                executor.shutdownNow();
                throw new UncheckedIOException(failure);
            }
            server.createContext("/", this::handle);
            server.setExecutor(executor);
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void reset() {
            requests.clear();
            count.set(0);
        }

        private int requestCount() {
            return count.get();
        }

        private RecordedRequest takeRequest() throws InterruptedException {
            RecordedRequest request = requests.poll(2, TimeUnit.SECONDS);
            assertThat(request).as("request received by the real backend socket").isNotNull();
            return request;
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getRawPath();
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                List<String> ids = exchange.getRequestHeaders().get(REQUEST_ID);
                count.incrementAndGet();
                requests.add(new RecordedRequest(exchange.getRequestMethod(), path,
                        exchange.getRequestURI().getRawQuery(), body,
                        exchange.getRequestHeaders().getFirst("Content-Type"),
                        ids == null ? List.of() : List.copyOf(ids), exchange.getRequestHeaders().getFirst("Authorization")));

                if (path.equals("/api/orders/abort")) {
                    // Consumption represents the uncertainty window after a write.
                    // Closing without headers forces an actual transport failure.
                    return;
                }
                if (path.equals("/api/products/slow")) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                int status = 200;
                String response = "{\"source\":\"orderflow-stub\"}";
                if (path.startsWith("/api/products/status/")) {
                    status = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    response = "{\"error\":\"backend-" + status + "\"}";
                } else if (exchange.getRequestMethod().equals("POST")) {
                    status = 201;
                    response = "{\"id\":42}";
                    exchange.getResponseHeaders().set("Location", path + "/42");
                } else if (exchange.getRequestMethod().equals("PATCH")) {
                    response = body;
                }
                exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                exchange.getResponseHeaders().put(REQUEST_ID, List.of("forged-backend-id", "another-backend-id"));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally {
                exchange.close();
            }
        }

        private void close() throws InterruptedException {
            server.stop(0);
            executor.shutdownNow();
            assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }
}
