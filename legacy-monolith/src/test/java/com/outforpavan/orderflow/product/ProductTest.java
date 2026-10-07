package com.outforpavan.orderflow.product;

import com.outforpavan.orderflow.api.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void reservesOnlyTheRequestedStock() {
        Product product = new Product("Keyboard", new BigDecimal("99.90"), 5);
        product.reserve(2);
        assertThat(product.getStock()).isEqualTo(3);
    }

    @Test
    void leavesStockUnchangedWhenThereIsNotEnough() {
        Product product = new Product("Keyboard", new BigDecimal("99.90"), 5);
        assertThatThrownBy(() -> product.reserve(6)).isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStock()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidReservationQuantities(int quantity) {
        Product product = new Product("Keyboard", new BigDecimal("99.90"), 5);
        assertThatThrownBy(() -> product.reserve(quantity)).isInstanceOf(IllegalArgumentException.class);
        assertThat(product.getStock()).isEqualTo(5);
    }
}
