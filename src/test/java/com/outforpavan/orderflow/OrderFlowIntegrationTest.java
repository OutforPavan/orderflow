package com.outforpavan.orderflow;

import com.outforpavan.orderflow.api.InsufficientStockException;
import com.outforpavan.orderflow.order.CreateOrderRequest;
import com.outforpavan.orderflow.order.OrderResponse;
import com.outforpavan.orderflow.order.OrderService;
import com.outforpavan.orderflow.product.CreateProductRequest;
import com.outforpavan.orderflow.product.ProductResponse;
import com.outforpavan.orderflow.product.ProductService;
import com.outforpavan.orderflow.product.UpdateProductPriceRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderFlowIntegrationTest {
    @Autowired ProductService products;
    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;
    private final List<Long> createdProducts = new ArrayList<>();

    @BeforeEach
    void requiresTheDedicatedTestDatabase() {
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .as("Integration drills must never run against the learning database")
                .isEqualTo("orderflow_test");
    }

    @AfterEach
    void removesOnlyDataCreatedByThisTest() {
        for (Long productId : createdProducts) {
            jdbc.update("delete from purchase_orders where product_id = ?", productId);
            jdbc.update("delete from products where id = ?", productId);
        }
    }

    @Test
    void commitsTheOrderAndStockTogether() {
        ProductResponse product = productWithStock(10);
        OrderResponse order = orders.create(new CreateOrderRequest(product.id(), 3));

        // There is no test-managed transaction: the service has already committed.
        assertThat(stock(product.id())).isEqualTo(7);
        assertThat(orderCount(product.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select total from purchase_orders where id = ?",
                BigDecimal.class, order.id())).isEqualByComparingTo("37.50");
        assertThat(orders.get(order.id()).productId()).isEqualTo(product.id());
    }

    @Test
    void rejectsInsufficientStockWithoutChangingDatabaseState() {
        ProductResponse product = productWithStock(2);
        assertThatThrownBy(() -> orders.create(new CreateOrderRequest(product.id(), 3)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stock(product.id())).isEqualTo(2);
        assertThat(orderCount(product.id())).isZero();
    }

    @Test
    void dirtyCheckingPersistsPriceAndTheExistingOrderKeepsItsPriceSnapshot() {
        ProductResponse product = productWithStock(10);
        OrderResponse order = orders.create(new CreateOrderRequest(product.id(), 2));

        products.changePrice(product.id(), new UpdateProductPriceRequest(new BigDecimal("19.95")));

        assertThat(jdbc.queryForObject("select price from products where id = ?",
                BigDecimal.class, product.id())).isEqualByComparingTo("19.95");
        assertThat(orders.get(order.id()).unitPrice()).isEqualByComparingTo("12.50");
        assertThat(orders.get(order.id()).total()).isEqualByComparingTo("25.00");
    }

    @Test
    void rollsBackExecutedStockSqlWhenTheOrderInsertFails() {
        ProductResponse product = productWithStock(10);
        String trigger = "day1_reject_order_" + product.id();
        // The function checks that the stock UPDATE is visible inside this transaction
        // before raising the injected failure. It exists only in the isolated test DB.
        jdbc.execute("""
                create function %s() returns trigger language plpgsql as $$
                begin
                    if NEW.product_id = %d then
                        if (select stock from products where id = NEW.product_id) <> 7 then
                            raise exception 'Stock UPDATE did not precede the order INSERT';
                        end if;
                        raise exception 'Injected Day 1 failure after stock UPDATE';
                    end if;
                    return NEW;
                end;
                $$
                """.formatted(trigger, product.id()));
        try {
            jdbc.execute("create trigger " + trigger + " after insert on purchase_orders "
                    + "for each row execute function " + trigger + "()");
            assertThatThrownBy(() -> orders.create(new CreateOrderRequest(product.id(), 3)))
                    .isInstanceOf(DataAccessException.class)
                    .hasStackTraceContaining("Injected Day 1 failure after stock UPDATE");

            // Fresh JDBC queries after the failed service transaction has ended.
            assertThat(stock(product.id())).isEqualTo(10);
            assertThat(orderCount(product.id())).isZero();
        } finally {
            jdbc.execute("drop trigger if exists " + trigger + " on purchase_orders");
            jdbc.execute("drop function if exists " + trigger + "()");
        }
    }

    private ProductResponse productWithStock(int stock) {
        ProductResponse product = products.create(new CreateProductRequest("Day 1 test product",
                new BigDecimal("12.50"), stock));
        createdProducts.add(product.id());
        return product;
    }

    private int stock(Long id) {
        return jdbc.queryForObject("select stock from products where id = ?", Integer.class, id);
    }

    private int orderCount(Long id) {
        return jdbc.queryForObject("select count(*) from purchase_orders where product_id = ?", Integer.class, id);
    }
}
