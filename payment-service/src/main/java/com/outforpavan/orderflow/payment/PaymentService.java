package com.outforpavan.orderflow.payment;

import com.outforpavan.orderflow.payment.provider.PaymentProvider;
import jakarta.validation.Validator;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentProvider provider;
    private final Validator validator;

    public PaymentService(PaymentRepository repository, PaymentProvider provider, Validator validator) {
        this.repository = repository;
        this.provider = provider;
        this.validator = validator;
    }

    @Transactional(timeout = 5)
    public PaymentResponse create(CreatePaymentRequest request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment request");
        }
        var normalized = new CreatePaymentRequest(request.orderId(), request.customerId(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY), request.paymentMethodReference());
        String fingerprint = fingerprint(normalized);
        repository.lockOrder(request.orderId());
        var existing = repository.find(request.orderId());
        if (existing.isPresent()) {
            if (!existing.get().fingerprint().equals(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The order already has a payment with different request data");
            }
            return existing.get().response();
        }

        UUID paymentId = UUID.randomUUID();
        var outcome = provider.charge(paymentId, normalized);
        var response = new PaymentResponse(normalized.orderId(), normalized.customerId(), normalized.amount(),
                outcome.status(), paymentId, outcome.reason());
        repository.save(response, fingerprint);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse find(UUID orderId) {
        return repository.find(orderId).map(PaymentRepository.StoredPayment::response)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse findForCustomer(UUID orderId, String customerId, boolean admin) {
        var response = find(orderId);
        if (!admin && !response.customerId().equals(customerId)) {
            // Same response as a missing ID: do not reveal another customer's payment.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return response;
    }

    private String fingerprint(CreatePaymentRequest request) {
        // Length-prefix strings so embedded separators cannot produce ambiguous input.
        String canonical = request.customerId().length() + ":" + request.customerId()
                + ":" + request.amount().toPlainString()
                + ":" + request.paymentMethodReference().length() + ":" + request.paymentMethodReference();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 algorithm unavailable", impossible);
        }
    }
}
