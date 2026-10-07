package com.outforpavan.orderflow.payment.provider;

import com.outforpavan.orderflow.payment.CreatePaymentRequest;
import com.outforpavan.orderflow.payment.PaymentStatus;
import java.util.UUID;

/** Adapter boundary for the provider. The supplied implementation is a local simulator. */
public interface PaymentProvider {
    Outcome charge(UUID paymentId, CreatePaymentRequest request);

    record Outcome(PaymentStatus status, String reason) {
    }
}
