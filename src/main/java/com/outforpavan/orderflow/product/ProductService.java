package com.outforpavan.orderflow.product;

import com.outforpavan.orderflow.api.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product(request.name(), request.price(), request.stock());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse changePrice(long id, UpdateProductPriceRequest request) {
        Product product = findProduct(id);
        product.changePrice(request.price());
        // The managed entity is dirty-checked; transaction commit flushes the update.
        return ProductResponse.from(product);
    }

    private Product findProduct(long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
    }
}
