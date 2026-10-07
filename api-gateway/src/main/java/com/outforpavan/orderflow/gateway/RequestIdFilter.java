package com.outforpavan.orderflow.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements WebFilter {
    static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Generate at the edge: callers cannot forge or inject text into our ID.
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        ServerWebExchange forwarded = exchange.mutate().request(request -> request.headers(
                headers -> headers.set(HEADER, requestId))).build();

        exchange.getResponse().beforeCommit(() -> {
            // set() also replaces any conflicting ID returned by the downstream.
            exchange.getResponse().getHeaders().set(HEADER, requestId);
            Route route = forwarded.getAttribute(GATEWAY_ROUTE_ATTR);
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("requestId={} route={} method={} status={} elapsedMs={}",
                    requestId, route == null ? "unmatched-or-local" : route.getId(),
                    exchange.getRequest().getMethod(), status == null ? 200 : status.value(),
                    (System.nanoTime() - started) / 1_000_000);
            return Mono.empty();
        });
        return chain.filter(forwarded);
    }
}
