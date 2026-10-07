package com.outforpavan.orderflow.order;

import com.outforpavan.orderflow.pricing.PriceBreakdown;
import com.outforpavan.orderflow.pricing.PriceCalculator;
import com.outforpavan.orderflow.pricing.ServiceLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 22, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", nullable = false, precision = 22, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "priority_surcharge_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal prioritySurchargePercent;

    @Column(name = "priority_surcharge_amount", nullable = false, precision = 22, scale = 2)
    private BigDecimal prioritySurchargeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_level", nullable = false, length = 20)
    private ServiceLevel serviceLevel;

    @Column(nullable = false, precision = 22, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(Long productId, int quantity, BigDecimal unitPrice) {
        this(productId, quantity, unitPrice,
                new PriceCalculator().calculate(unitPrice, quantity, BigDecimal.ZERO, ServiceLevel.STANDARD));
    }

    public PurchaseOrder(Long productId, int quantity, BigDecimal unitPrice, PriceBreakdown price) {
        if (productId == null || productId.longValue() <= 0 || quantity <= 0 || unitPrice == null
                || unitPrice.signum() <= 0 || price == null
                || price.getSubtotal().compareTo(unitPrice.multiply(BigDecimal.valueOf(quantity))) != 0) {
            throw new IllegalArgumentException("Order product, quantity and price breakdown must agree");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = price.getSubtotal();
        this.discountPercent = price.getDiscountPercent();
        this.discountAmount = price.getDiscountAmount();
        this.prioritySurchargePercent = price.getPrioritySurchargePercent();
        this.prioritySurchargeAmount = price.getPrioritySurchargeAmount();
        this.serviceLevel = price.getServiceLevel();
        this.total = price.getTotal();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPrioritySurchargePercent() { return prioritySurchargePercent; }
    public BigDecimal getPrioritySurchargeAmount() { return prioritySurchargeAmount; }
    public ServiceLevel getServiceLevel() { return serviceLevel; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
}
