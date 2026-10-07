package com.outforpavan.orderflow.inventory.product;

import com.outforpavan.orderflow.inventory.availability.AvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository products;
    private final AvailabilityService availability;

    public ProductService(ProductRepository products, AvailabilityService availability) {
        this.products = products;
        this.availability = availability;
    }

    @Transactional(timeout = 3)
    public Product create(CreateProductRequest request) {
        return products.insert(request);
    }

    public Product get(long id) {
        return products.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional(timeout = 3)
    public Product changePrice(long id, ChangePriceRequest request) {
        Product previous = products.lock(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        products.updatePrice(id, request.price());
        availability.evictAfterCommit(id);
        return new Product(id, previous.name(), request.price(), previous.stock());
    }
}
