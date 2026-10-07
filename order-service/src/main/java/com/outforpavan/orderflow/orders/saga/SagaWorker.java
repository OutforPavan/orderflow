package com.outforpavan.orderflow.orders.saga;

import com.outforpavan.orderflow.orders.client.InventoryClient;
import com.outforpavan.orderflow.orders.client.PaymentClient;
import com.outforpavan.orderflow.orders.persistence.OrderStore;
import com.outforpavan.orderflow.orders.persistence.OrderStore.StoredOrder;
import com.outforpavan.orderflow.pricing.PriceCalculator;
import com.outforpavan.orderflow.pricing.ProductDiscountPolicy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/** Durable orchestration: every network step has an idempotent participant operation. */
@Service
public class SagaWorker {
    private final OrderStore orders;
    private final InventoryClient inventory;
    private final PaymentClient payments;
    private final PriceCalculator calculator;
    private final ProductDiscountPolicy discounts;

    public SagaWorker(OrderStore orders, InventoryClient inventory, PaymentClient payments,
                      PriceCalculator calculator, ProductDiscountPolicy discounts) {
        this.orders = orders;
        this.inventory = inventory;
        this.payments = payments;
        this.calculator = calculator;
        this.discounts = discounts;
    }

    public boolean processOnce() {
        var claimed = orders.claim();
        if (claimed.isEmpty()) return false;
        StoredOrder order = claimed.get();
        try {
            switch (order.view().state()) {
                case "RESERVE_PENDING" -> reserve(order);
                case "PAYMENT_PENDING" -> pay(order);
                case "RELEASE_PENDING" -> release(order);
                default -> throw new IllegalStateException("Unclaimable state");
            }
        } catch (HttpClientErrorException error) {
            // A contract or permission failure needs investigation; never guess whether money moved.
            orders.advance(order, "REVIEW_REQUIRED", "Participant rejected request: HTTP " + error.getStatusCode().value());
        } catch (CallNotPermittedException openCircuit) {
            orders.retry(order, "Participant circuit is open; outcome remains pending");
        } catch (RuntimeException unknownOutcome) {
            // Includes a response lost AFTER a participant committed. Repeat the SAME operation/key.
            orders.retry(order, "Participant outcome unknown; retry scheduled");
        }
        return true;
    }

    private void reserve(StoredOrder order) {
        var view = order.view();
        var result = inventory.reserve(view.id(), view.productId(), view.quantity());
        require(Objects.equals(result.orderId(), view.id()), "Reservation order mismatch");
        require(Objects.equals(result.productId(), view.productId()) && Objects.equals(result.quantity(), view.quantity()),
                "Reservation payload mismatch");
        switch (result.status()) {
            case "RESERVED" -> orders.reserved(order, result.unitPrice(), calculator.calculate(result.unitPrice(),
                    view.quantity(), discounts.getDiscountPercent(view.productId()), view.serviceLevel()));
            case "REJECTED" -> orders.advance(order, "REJECTED", "Inventory rejected reservation");
            case "RELEASED" -> orders.advance(order, "REVIEW_REQUIRED", "Reservation was already released");
            default -> throw new IllegalStateException("Unknown reservation status");
        }
    }

    private void pay(StoredOrder order) {
        var view = order.view();
        var result = payments.pay(new PaymentClient.PayRequest(view.id(), view.customerId(), view.total(),
                order.paymentMethodReference()));
        require(Objects.equals(result.orderId(), view.id()) && Objects.equals(result.customerId(), view.customerId())
                && result.amount() != null && result.amount().compareTo(view.total()) == 0, "Payment payload mismatch");
        switch (result.status()) {
            case "SUCCEEDED" -> orders.advance(order, "CONFIRMED", null);
            case "DECLINED" -> orders.advance(order, "RELEASE_PENDING", "Payment declined; release stock");
            case "PENDING" -> orders.retry(order, "Payment outcome pending");
            default -> throw new IllegalStateException("Unknown payment status");
        }
    }

    private void release(StoredOrder order) {
        var result = inventory.release(order.view().id());
        require(Objects.equals(result.orderId(), order.view().id()) && "RELEASED".equals(result.status()),
                "Release not confirmed");
        orders.advance(order, "CANCELLED", "Payment declined; stock released");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
