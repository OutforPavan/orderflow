package com.outforpavan.orderflow.gateway;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
public class GatewayHttpClientConfiguration {
    @Bean
    HttpClientCustomizer disableTransportRetry() {
        // Reactor Netty can retry an aborted connection even without a Retry filter.
        // An order POST may already have committed; replay needs durable idempotency.
        return httpClient -> httpClient.disableRetry(true);
    }

    @Bean
    @ConditionalOnProperty(name = "platform.gateway.backend-tls-enabled", matchIfMissing = true)
    HttpClientCustomizer trustedMutualTls(SslBundles bundles) throws Exception {
        var sslContext = sslContext(bundles);
        return client -> client.secure(spec -> spec.sslContext(sslContext));
    }

    static HttpClient secureClient(HttpClient client, SslBundles bundles) throws Exception {
        var sslContext = sslContext(bundles);
        // HttpClient.secure(Consumer) retains HTTPS endpoint hostname verification.
        return client.secure(spec -> spec.sslContext(sslContext));
    }

    private static io.netty.handler.ssl.SslContext sslContext(SslBundles bundles) throws Exception {
        SslBundle bundle = bundles.getBundle("client");
        return SslContextBuilder.forClient()
                .keyManager(bundle.getManagers().getKeyManagerFactory())
                .trustManager(bundle.getManagers().getTrustManagerFactory()).build();
    }
}
