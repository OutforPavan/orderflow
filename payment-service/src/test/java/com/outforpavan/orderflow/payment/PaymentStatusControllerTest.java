package com.outforpavan.orderflow.payment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PaymentStatusControllerTest {
    private final PaymentService service = mock(PaymentService.class);
    private final PaymentStatusController controller = new PaymentStatusController(service);

    @Test
    void customerIdentityComesFromTheVerifiedJwtSubject() {
        UUID orderId = UUID.randomUUID();
        controller.status(orderId, authentication("alice-subject", "ROLE_customer"));
        verify(service).findForCustomer(orderId, "alice-subject", false);
    }

    @Test
    void onlyTheMappedAdminAuthorityEnablesAdminRead() {
        UUID orderId = UUID.randomUUID();
        controller.status(orderId, authentication("admin-subject", "ROLE_admin"));
        verify(service).findForCustomer(orderId, "admin-subject", true);
    }

    @Test
    void callingYourselfAdminDoesNotGrantAdminAuthority() {
        UUID orderId = UUID.randomUUID();
        controller.status(orderId, authentication("admin", "ROLE_customer"));
        verify(service).findForCustomer(orderId, "admin", false);
    }

    private JwtAuthenticationToken authentication(String subject, String role) {
        Jwt jwt = Jwt.withTokenValue("unit-test-token")
                .header("alg", "RS256").subject(subject).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
    }
}
