package com.outforpavan.orderflow.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ServiceSecurityTest.Application.class)
@AutoConfigureMockMvc
class ServiceSecurityTest {
    @Autowired MockMvc mvc;
    @TestBean(name = "serviceJwtDecoder", methodName = "fixtureDecoder") JwtDecoder decoder;

    static JwtDecoder fixtureDecoder() {
        // Signed-token authentication is exercised separately by the gateway HTTP tests.
        return value -> {
            return Jwt.withTokenValue(value).header("alg", "RS256").subject("alice")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                    .claim("realm_access", Map.of("roles", List.of(value))).build();
        };
    }

    @Test
    void onlyOrderCertificateCanInvokeInternalOperations() throws Exception {
        mvc.perform(post("/internal/reservations").requestAttr("jakarta.servlet.request.X509Certificate", certificate("order-service")))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/reservations").requestAttr("jakarta.servlet.request.X509Certificate", certificate("api-gateway")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/reservations").header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void serviceCertificateCannotReplaceEndUserToken() throws Exception {
        mvc.perform(get("/api/orders/42").requestAttr("jakarta.servlet.request.X509Certificate", certificate("order-service")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/42").header("Authorization", "Bearer customer"))
                .andExpect(status().isOk());
    }

    @Test
    void publicMutationsRequireAdminEvenBehindGateway() throws Exception {
        mvc.perform(post("/api/products").header("Authorization", "Bearer customer"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/products").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        mvc.perform(get("/unmapped").header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
    }

    private X509Certificate[] certificate(String app) throws Exception {
        Path path = Path.of(System.getProperty("platform.root"), ".tools/platform/pki", app + ".crt");
        try (InputStream stream = Files.newInputStream(path)) {
            return new X509Certificate[]{(X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream)};
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ServiceSecurityConfiguration.class, Endpoints.class})
    static class Application { }

    @RestController
    static class Endpoints {
        @PostMapping({"/internal/reservations", "/api/products"}) String write() { return "ok"; }
        @GetMapping("/api/orders/42") String read() { return "ok"; }
    }
}
