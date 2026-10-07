package com.outforpavan.orderflow.gateway;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GatewayHttpClientConfiguration {
    @Bean
    HttpClientCustomizer disableTransportRetry() {
        // Reactor Netty can retry an aborted connection even without a Retry filter.
        // An order POST may already have committed; replay needs durable idempotency.
        return httpClient -> httpClient.disableRetry(true);
    }
}
