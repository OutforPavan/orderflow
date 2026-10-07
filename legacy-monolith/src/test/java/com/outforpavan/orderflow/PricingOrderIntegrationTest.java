package com.outforpavan.orderflow;

import com.outforpavan.orderflow.api.InsufficientStockException;
import com.outforpavan.orderflow.order.CreateOrderRequest;
import com.outforpavan.orderflow.order.OrderResponse;
import com.outforpavan.orderflow.order.OrderService;
import com.outforpavan.orderflow.pricing.ServiceLevel;
import com.outforpavan.orderflow.product.CreateProductRequest;
import com.outforpavan.orderflow.product.ProductResponse;
import com.outforpavan.orderflow.product.ProductService;
import com.outforpavan.orderflow.product.UpdateProductPriceRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "pricing.discount-all-products=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PricingOrderIntegrationTest {
    @Autowired ProductService products;
    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private final List<Long> createdProducts = new ArrayList<>();

    @BeforeEach
    void requiresTheDedicatedTestDatabase() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("orderflow_test");
    }

    @AfterEach
    void removesOnlyThisTestsData() {
        for (Long id : createdProducts) {
            jdbc.update("delete from purchase_orders where product_id = ?", id);
            jdbc.update("delete from products where id = ?", id);
        }
    }

    @Test
    void sameProductSupportsDiscountAndBothPriorityFeesWithoutChangingCatalogPrice() {
        ProductResponse product = product("1000.00", 10);
        OrderResponse standard = orders.create(new CreateOrderRequest(product.id(), 1));
        OrderResponse five = orders.create(new CreateOrderRequest(product.id(), 1, ServiceLevel.PRIORITY_5));
        OrderResponse ten = orders.create(new CreateOrderRequest(product.id(), 1, ServiceLevel.PRIORITY_10));

        assertThat(standard.total()).isEqualByComparingTo("900.00");
        assertThat(five.total()).isEqualByComparingTo("945.00");
        assertThat(ten.total()).isEqualByComparingTo("990.00");
        assertThat(orders.get(five.id()).prioritySurchargeAmount()).isEqualByComparingTo("45.00");
        assertThat(orders.get(ten.id()).serviceLevel()).isEqualTo(ServiceLevel.PRIORITY_10);
        assertThat(products.get(product.id()).price()).isEqualByComparingTo("1000.00");
        assertThat(products.get(product.id()).stock()).isEqualTo(7);
    }

    @Test
    void orderRetainsItsCompletePriceSnapshotAfterCatalogPriceChanges() {
        ProductResponse product = product("1000.00", 10);
        OrderResponse saved = orders.create(new CreateOrderRequest(product.id(), 2, ServiceLevel.PRIORITY_10));
        products.changePrice(product.id(), new UpdateProductPriceRequest(new BigDecimal("2000.00")));
        OrderResponse old = orders.get(saved.id());

        assertThat(old.unitPrice()).isEqualByComparingTo("1000.00");
        assertThat(old.subtotal()).isEqualByComparingTo("2000.00");
        assertThat(old.discountAmount()).isEqualByComparingTo("200.00");
        assertThat(old.prioritySurchargeAmount()).isEqualByComparingTo("180.00");
        assertThat(old.total()).isEqualByComparingTo("1980.00");
    }

    @Test
    void databaseEnforcesTheSameRoundingAndRejectsInconsistentTotals() {
        ProductResponse product = product("0.05", 3);
        OrderResponse order = orders.create(new CreateOrderRequest(product.id(), 3, ServiceLevel.PRIORITY_5));
        assertThat(orders.get(order.id()).total()).isEqualByComparingTo("0.14");
        assertThatThrownBy(() -> jdbc.update("update purchase_orders set total = total + 1 where id = ?", order.id()))
                .isInstanceOf(DataAccessException.class);
        assertThat(orders.get(order.id()).total()).isEqualByComparingTo("0.14");
    }

    @Test
    void realApiReturnsThePersistedBreakdown() throws Exception {
        ProductResponse product = product("1000.00", 3);
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"productId\":" + product.id() + ",\"quantity\":1,\"serviceLevel\":\"PRIORITY_5\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1000))
                .andExpect(jsonPath("$.discountAmount").value(100))
                .andExpect(jsonPath("$.prioritySurchargeAmount").value(45))
                .andExpect(jsonPath("$.total").value(945));
        Long id = jdbc.queryForObject("select id from purchase_orders where product_id = ?", Long.class, product.id());
        mvc.perform(get("/api/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(945))
                .andExpect(jsonPath("$.serviceLevel").value("PRIORITY_5"));
    }

    @Test
    void concurrentCustomersCannotBothBuyTheLastItem() throws Exception {
        ProductResponse product = product("1000.00", 1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> buy = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                try {
                    orders.create(new CreateOrderRequest(product.id(), 1, ServiceLevel.PRIORITY_5));
                    return true;
                } catch (InsufficientStockException expected) {
                    return false;
                }
            };
            Future<Boolean> first = workers.submit(buy);
            Future<Boolean> second = workers.submit(buy);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            boolean firstWon = first.get(10, TimeUnit.SECONDS);
            boolean secondWon = second.get(10, TimeUnit.SECONDS);
            assertThat(firstWon ^ secondWon).isTrue();
            assertThat(products.get(product.id()).stock()).isZero();
            assertThat(jdbc.queryForObject("select count(*) from purchase_orders where product_id = ?",
                    Integer.class, product.id())).isEqualTo(1);
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ProductResponse product(String price, int stock) {
        ProductResponse product = products.create(new CreateProductRequest("Pricing interview test", new BigDecimal(price), stock));
        createdProducts.add(product.id());
        return product;
    }
}
