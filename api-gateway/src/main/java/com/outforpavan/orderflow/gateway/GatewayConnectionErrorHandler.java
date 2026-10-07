package com.outforpavan.orderflow.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2) // Translate before Spring Boot's default error handler renders JSON.
public class GatewayConnectionErrorHandler implements WebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable failure) {
        if (exchange.getResponse().isCommitted() || failure instanceof ResponseStatusException) {
            return Mono.error(failure);
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
            }
        }
        // Preserve native 504 timeouts and ordinary application errors.
        return Mono.error(failure);
    }
}
