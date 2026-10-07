package com.outforpavan.orderflow.security;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class ServiceSecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder serviceJwtDecoder(SslBundles bundles,
            @Value("${platform.security.issuer:https://localhost:8443/realms/orderflow}") String issuer,
            @Value("${platform.security.audience:orderflow-api}") String audience) {
        HttpClient client = HttpClient.newBuilder().sslContext(bundles.getBundle("client").createSslContext())
                .connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(client);
        requests.setReadTimeout(Duration.ofSeconds(2));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/protocol/openid-connect/certs")
                .jwsAlgorithm(SignatureAlgorithm.RS256).restOperations(new RestTemplate(requests)).build();
        decoder.setJwtValidator(TokenPolicy.validator(issuer, audience));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain internalServiceRequests(HttpSecurity http) throws Exception {
        // Separate chain deliberately has NO bearer-token authentication. TLS validates the CA;
        // x509 binds this operation's caller to the exact order-service certificate subject.
        http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("SERVICE_ORDER"))
                .x509(x509 -> x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)").userDetailsService(name -> {
                    if (!"order-service".equals(name)) {
                        throw new UsernameNotFoundException("Certificate identity is not authorized for internal operations");
                    }
                    return User.withUsername(name).password("unused").roles("SERVICE_ORDER").build();
                }));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain publicServiceRequests(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(TokenPolicy::authorities);
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/orders/_operations/**").hasRole("admin")
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasRole("admin")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("admin")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**",
                                "/api/inventory/availability/**", "/api/orders", "/api/orders/**",
                                "/api/payments/status/**").hasAnyRole("customer", "admin")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("customer", "admin")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
