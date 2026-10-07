package com.outforpavan.orderflow.order;

import com.outforpavan.orderflow.api.ResourceNotFoundException;
import com.outforpavan.orderflow.product.Product;
import com.outforpavan.orderflow.product.ProductRepository;
import com.outforpavan.orderflow.pricing.PriceBreakdown;
import com.outforpavan.orderflow.pricing.PriceCalculator;
import com.outforpavan.orderflow.pricing.ProductDiscountPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PriceCalculator calculator;
    private final ProductDiscountPolicy discounts;

    public OrderService(ProductRepository products, OrderRepository orders,
                        PriceCalculator calculator, ProductDiscountPolicy discounts) {
        this.products = products;
        this.orders = orders;
        this.calculator = calculator;
        this.discounts = discounts;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + request.productId() + " not found"));
        PriceBreakdown price = calculator.calculate(product.getPrice(), request.quantity(),
                discounts.getDiscountPercent(product.getId()), request.serviceLevel());
        product.reserve(request.quantity());

        // Deliberate for the Day 1 lab: execute the stock UPDATE before the INSERT.
        // Flush sends SQL; it does not commit this transaction.
        products.flush();
        PurchaseOrder order = orders.saveAndFlush(
                new PurchaseOrder(product.getId(), request.quantity(), product.getPrice(), price));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long id) {
        return orders.findById(id).map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }
}
