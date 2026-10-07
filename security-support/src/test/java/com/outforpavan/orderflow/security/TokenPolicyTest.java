package com.outforpavan.orderflow.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class TokenPolicyTest {
    @Test
    void onlyKnownUserRolesAreMappedNeverInternalServiceAuthority() {
        Jwt jwt = valid().claim("realm_access", Map.of("roles", List.of("admin", "customer", "SERVICE_ORDER", "unknown"))).build();
        assertThat(TokenPolicy.authorities(jwt)).extracting("authority")
                .containsExactly("ROLE_admin", "ROLE_customer");
    }

    @Test
    void malformedOrAbsentRoleClaimsGiveNoAuthority() {
        assertThat(TokenPolicy.authorities(valid().build())).isEmpty();
        assertThat(TokenPolicy.authorities(valid().claim("realm_access", "admin").build())).isEmpty();
        assertThat(TokenPolicy.authorities(valid().claim("realm_access", Map.of("roles", "admin")).build())).isEmpty();
    }

    @Test
    void audienceSubjectAndExpirationAreMandatory() {
        var validator = TokenPolicy.validator("https://issuer", "orderflow-api");
        assertThat(validator.validate(valid().build()).hasErrors()).isFalse();
        assertThat(validator.validate(valid().audience(List.of("another-api")).build()).hasErrors()).isTrue();
        assertThat(validator.validate(valid().subject("").build()).hasErrors()).isTrue();
        Jwt noExpiry = Jwt.withTokenValue("test").header("alg", "RS256").issuer("https://issuer")
                .subject("alice").audience(List.of("orderflow-api")).build();
        assertThat(validator.validate(noExpiry).hasErrors()).isTrue();
    }

    private Jwt.Builder valid() {
        return Jwt.withTokenValue("test").header("alg", "RS256").issuer("https://issuer")
                .subject("alice").audience(List.of("orderflow-api"))
                .issuedAt(Instant.now().minusSeconds(5)).expiresAt(Instant.now().plusSeconds(300));
    }
}
