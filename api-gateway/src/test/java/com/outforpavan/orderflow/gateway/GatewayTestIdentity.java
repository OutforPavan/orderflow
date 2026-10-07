package com.outforpavan.orderflow.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.outforpavan.orderflow.security.TokenPolicy;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/** Real RSA signature validation, with test keys only; runtime still uses Keycloak JWKS. */
@TestConfiguration(proxyBeanMethods = false)
class GatewayTestIdentity {
    static final String ISSUER = "https://localhost:8443/realms/orderflow";
    static final RSAKey KEY = key();
    private static RSAKey key() {
        try {
            return new RSAKeyGenerator(2048).keyID("gateway-test").generate();
        } catch (JOSEException error) {
            throw new IllegalStateException(error);
        }
    }

    @Bean
    @Primary
    ReactiveJwtDecoder testJwtDecoder() throws JOSEException {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
        decoder.setJwtValidator(TokenPolicy.validator(ISSUER, "orderflow-api"));
        return decoder;
    }

    static String token(String role) {
        return token(role, ISSUER, "orderflow-api", Instant.now().plusSeconds(300));
    }

    static String token(String role, String issuer, String audience, Instant expires) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(),
                    new JWTClaimsSet.Builder().issuer(issuer).subject("test-user").audience(audience)
                            .issueTime(Date.from(Instant.now().minusSeconds(600))).expirationTime(Date.from(expires))
                            .claim("realm_access", Map.of("roles", List.of(role))).build());
            jwt.sign(new RSASSASigner(KEY));
            return jwt.serialize();
        } catch (JOSEException error) {
            throw new IllegalStateException(error);
        }
    }
}
