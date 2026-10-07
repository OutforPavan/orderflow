package com.outforpavan.orderflow.gateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayUnavailableBackendTest {
    // Reserve a TCP port without listening: deterministic refusal without racing
    // another process to reuse a recently closed server port.
    private static final Socket UNAVAILABLE = reservePort();

    @LocalServerPort
    private int gatewayPort;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry properties) {
        properties.add("ORDERFLOW_URL", () -> "http://127.0.0.1:" + UNAVAILABLE.getLocalPort());
    }

    @Test
    void reports502WhenBackendIsUnavailableWhileLocalHealthRemainsUp() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + gatewayPort)
                .responseTimeout(Duration.ofSeconds(5)).build();
        client.get().uri("/api/products/42").exchange()
                .expectStatus().isEqualTo(502)
                .expectHeader().value("X-Request-Id", id ->
                        assertThat(UUID.fromString(id).toString()).isEqualTo(id))
                .expectBody().jsonPath("$.status").isEqualTo(502)
                .jsonPath("$.trace").doesNotExist();
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @AfterAll
    static void releasePort() throws IOException {
        UNAVAILABLE.close();
    }

    private static Socket reservePort() {
        try {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress("127.0.0.1", 0));
            return socket;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
