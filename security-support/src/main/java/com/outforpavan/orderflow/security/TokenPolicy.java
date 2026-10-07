package com.outforpavan.orderflow.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Same claim validation and deliberate role allowlist at the gateway and services. */
public final class TokenPolicy {
    private TokenPolicy() { }

    public static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> required = jwt -> {
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Required subject, expiration or audience is missing or invalid", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), required);
    }

    public static Collection<GrantedAuthority> authorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access) || !(access.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        // A signed user JWT must never turn a role claim into a service-certificate identity.
        return roles.stream().filter(role -> "customer".equals(role) || "admin".equals(role))
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).distinct().toList();
    }
}
