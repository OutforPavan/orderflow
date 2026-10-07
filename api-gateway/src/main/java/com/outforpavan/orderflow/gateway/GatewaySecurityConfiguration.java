package com.outforpavan.orderflow.gateway;

import com.outforpavan.orderflow.security.TokenPolicy;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
public class GatewaySecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    ReactiveJwtDecoder gatewayJwtDecoder(SslBundles bundles,
            @Value("${platform.security.issuer}") String issuer,
            @Value("${platform.security.audience}") String audience) throws Exception {
        HttpClient client = GatewayHttpClientConfiguration.secureClient(HttpClient.create(), bundles)
                .responseTimeout(Duration.ofSeconds(2));
        WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(issuer + "/protocol/openid-connect/certs")
                .webClient(webClient).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(TokenPolicy.validator(issuer, audience));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(TokenPolicy::authorities);
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/api/orders/_operations/**").hasRole("admin")
                        .pathMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasRole("admin")
                        .pathMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("admin")
                        .pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**",
                                "/api/inventory/availability/**", "/api/orders", "/api/orders/**",
                                "/api/payments/status/**").hasAnyRole("customer", "admin")
                        .pathMatchers(HttpMethod.POST, "/api/orders", "/api/orders/**").hasAnyRole("customer", "admin")
                        .anyExchange().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .build();
    }
}
